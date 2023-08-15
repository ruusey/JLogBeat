package com.jlogbeat.ingest.windows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.jlogbeat.entity.WindowsLog;
import com.jlogbeat.ingest.windows.model.EventLog;
import com.jlogbeat.repo.WinLogEventRepository;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Component
public class WinlogIngest {
	private static final List<String> LOG_NAMES = Arrays.asList("system", "application", "security");
	public static final String EXPORT_PATH = System.getenv("EXPORT_DIR") + "\\^&.json";
	public static final Long initialTs = Instant.now().toEpochMilli();
	private static final String COMMAND = "pwsh.exe -Command \"Get-WinEvent -FilterHashtable @{ LogName='^'; StartTime=+} -MaxEvents 100 | ConvertTo-Json -Depth 4 | Out-File % -Encoding utf8\"";

	public static final String FIREWALL_LOG = "C:\\Windows\\System32\\LogFiles\\Firewall\\pfirewall.log";

	public static final String DESTINATION_INDEX = "filebeat-2022.09.22-000001";
	private final transient ExecutorService executorService;
	private final transient WinLogEventRepository eventLogRepo;
	private final transient ImportThread firewallImport;

	private Map<String, Long> lastTs = new HashMap<>();

	public WinlogIngest(@Autowired ExecutorService executorService, @Autowired WinLogEventRepository eventLogRepo,
			@Autowired ImportThread firewallImport) {
		this.executorService = executorService;
		this.eventLogRepo = eventLogRepo;
		this.firewallImport = firewallImport;
	}

	public Long exportLogsToCsv() {
		Boolean initial = false;
		if (this.lastTs.isEmpty()) {
			for (String log : WinlogIngest.LOG_NAMES) {
				this.lastTs.put(log, WinlogIngest.initialTs);

			}
			initial = true;
		}
		List<ExportThread> threads = new ArrayList<>();
		Long dumpTimestamp = Instant.now().toEpochMilli();

		for (String log : WinlogIngest.LOG_NAMES) {
			Long diff = (dumpTimestamp - this.lastTs.get(log));
			String command = null;
			if (initial) {
				command = WinlogIngest.COMMAND
						.replace("+", "(Get-Date) - (New-TimeSpan -Hours 2)")
						.replace("%", WinlogIngest.EXPORT_PATH).replace("^", log).replace("&", "-" + dumpTimestamp + "");
			} else {
				command = WinlogIngest.COMMAND
						.replace("+", "(Get-Date) - (New-TimeSpan -Milliseconds " + (diff) + ")")
						.replace("%", WinlogIngest.EXPORT_PATH).replace("^", log).replace("&", "-" + dumpTimestamp + "");
			}

			ExportThread export = new ExportThread(command);
			this.executorService.execute(export);
			// export.start();
			threads.add(export);
		}

		Integer attempts = 0;
		List<ExportThread> notComplete = threads.stream().filter(thread->!thread.complete).collect(Collectors.toList());
		while ((notComplete.size() > 0) && !Thread.interrupted() && (attempts != 3)
				&& !this.executorService.isShutdown()) {
			WinlogIngest.log.info("Waiting on {} threads to complete", notComplete.size());

			notComplete = threads.stream().filter(thread->!thread.complete).collect(Collectors.toList());
			try {
				Thread.sleep(1500);
				attempts++;
			} catch (Exception e) {
			}
		}
		return dumpTimestamp;
	}

	public Long parseCsvAndIngest(Long dumpTimestamp) throws Exception {
		Long recordCount = 0l;
		WinlogIngest.log.info("Begin Windows CSV Log Ingest from Dump Timestamp {}", dumpTimestamp);
		ObjectMapper om = new ObjectMapper();
		om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		om.coercionConfigFor(LogicalType.POJO).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
		for(String logName : WinlogIngest.LOG_NAMES) {
			String filePath = WinlogIngest.EXPORT_PATH.replace("^", logName).replace("&", "-" + dumpTimestamp.toString());
			String content = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
			if ((content == null) || content.isBlank()) {
				try {
					FileUtils.delete(new File(filePath));
				} catch (Exception e) {
					WinlogIngest.log.error("Failed to delete temp dump files {}", e.getMessage());
				}
				continue;
			}
			WinlogIngest.log.info("Parsing file [{}]  ", filePath);

			EventLog[] root = om.readValue(content, EventLog[].class);
			this.lastTs.put(logName, dumpTimestamp);
			for (EventLog r : root) {
				try {
					String[] props = r.properties.stream().map(p -> p.value.toString()).toArray(String[]::new);
					WindowsLog winLog = WindowsLog.builder()
							.eventId(r.getId())
							.machineName(r.getMachineName())
							.dataStr(r.getProviderName())
							.idx(r.getRecordId())
							.category(r.getLogName()).entryType(r.getContainerLog()).message(r.getMessage())
							.sourceStr(logName)
							.replacementStrings(Stream.of(props).collect(Collectors.joining(",")))
							.instanceId(r.getProviderId())
							.timeGenerated(new Timestamp(r.getTimeCreated().getTime()))
							.build();
					winLog = this.eventLogRepo.save(winLog);
					recordCount++;
				}catch(Exception e) {
					WinlogIngest.log.error("Error saving EventLog {} Reason: {}", logName, e);
					continue;
				}
			}

			try {
				FileUtils.delete(new File(filePath));
			}catch(Exception e) {
				WinlogIngest.log.error("Failed to delete temp dump files {}", e.getMessage());
			}
		}
		return recordCount;
	}

	public void runRoutine() {
		this.executorService.execute(this.firewallImport);
		while (!this.executorService.isShutdown()) {

			Long dumpTs = this.exportLogsToCsv();
			WinlogIngest.log.info("Completed dump with Timestamp {} ", dumpTs);

			try {
				Long records = this.parseCsvAndIngest(dumpTs);
				WinlogIngest.log.info("Ingest {} Windows Event Logs complete, waiting 60 seconds...", records);
				Thread.sleep(60000);
			} catch (Exception e) {
				WinlogIngest.log.error("Failed to ingest CSV log dump. Reason: {}", e);
			}
		}
	}
}
