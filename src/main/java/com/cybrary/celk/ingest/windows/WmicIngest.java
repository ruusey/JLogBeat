package com.cybrary.celk.ingest.windows;

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

import com.cybrary.celk.ingest.windows.model.Root;
import com.cybrary.celk.ingest.windows.model.WindowsLog;
import com.cybrary.celk.repo.WinLogEventRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Component
public class WmicIngest {
	private static final List<String> LOG_NAMES = Arrays.asList("system", "application", "security");
	public static final String EXPORT_PATH = "D:\\tmp\\^&.json";
	public static final Long initialTs = Instant.now().toEpochMilli();
	private static final String COMMAND = "pwsh.exe -Command \"Get-WinEvent -FilterHashtable @{ LogName='^'; StartTime=+} -MaxEvents 100 | ConvertTo-Json -Depth 4 | Out-File % -Encoding utf8\"";

	public static final String FIREWALL_LOG = "C:\\Windows\\System32\\LogFiles\\Firewall\\pfirewall.log";

	public static final String DESTINATION_INDEX = "filebeat-2022.09.22-000001";
	private final transient ExecutorService executorService;
	private final transient WinLogEventRepository eventLogRepo;
	// private Long lastTs;
	private Map<String, Long> lastTs = new HashMap<>();
	public WmicIngest(@Autowired ExecutorService executorService, @Autowired WinLogEventRepository eventLogRepo) {
		this.executorService = executorService;
		this.eventLogRepo = eventLogRepo;
	}


	public ImportThread beginFirewallIngest() {
		ImportThread it = new ImportThread();
		return it;
	}

	public Long exportLogsToCsv() {
		Boolean initial = false;
		if (this.lastTs.isEmpty()) {
			for (String log : WmicIngest.LOG_NAMES) {
				this.lastTs.put(log, WmicIngest.initialTs);

			}
			initial = true;
		}
		List<ExportThread> threads = new ArrayList<>();
		Long dumpTimestamp = Instant.now().toEpochMilli();

		for (String log : WmicIngest.LOG_NAMES) {
			Long diff = (dumpTimestamp - this.lastTs.get(log));
			String command = null;
			if (initial) {
				command = WmicIngest.COMMAND
						.replace("+", "(Get-Date) - (New-TimeSpan -Days 2)")
						.replace("%", WmicIngest.EXPORT_PATH).replace("^", log).replace("&", "-" + dumpTimestamp + "");
			} else {
				command = WmicIngest.COMMAND
						.replace("+", "(Get-Date) - (New-TimeSpan -Milliseconds " + (diff) + ")")
						.replace("%", WmicIngest.EXPORT_PATH).replace("^", log).replace("&", "-" + dumpTimestamp + "");
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
			WmicIngest.log.info("Waiting on {} threads to complete", notComplete.size());

			notComplete = threads.stream().filter(thread->!thread.complete).collect(Collectors.toList());
			try {
				Thread.sleep(5000);
				attempts++;
			} catch (Exception e) {
			}
		}

		return dumpTimestamp;

	}

	public Long parseCsvAndIngest(Long dumpTimestamp) throws Exception {
		Long recordCount = 0l;
		WmicIngest.log.info("Begin Windows CSV Log Ingest from Dump Timestamp {}",dumpTimestamp);
		ObjectMapper om = new ObjectMapper();
		om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		om.coercionConfigFor(LogicalType.POJO).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
		for(String logName : WmicIngest.LOG_NAMES) {
			String filePath = WmicIngest.EXPORT_PATH.replace("^", logName).replace("&", "-" + dumpTimestamp.toString());
			String content = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
			if ((content == null) || content.isBlank()) {
				continue;
			}
			WmicIngest.log.info("Parsing file [{}]  ", filePath);

			Root[] root = om.readValue(content, Root[].class);
			this.lastTs.put(logName, dumpTimestamp);
			List<WindowsLog> logs = new ArrayList<>();
			for (Root r : root) {
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
					WmicIngest.log.warn("Error saving EventLog {} Reason: {}", logName, e.getMessage());
					continue;
				}
			}


			try {
				new File(filePath).delete();
				// this.eventLogRepo.saveAll(logs);

			}catch(Exception e) {
				WmicIngest.log.error("Failed to save all logs {}", e.getMessage());
			}
		}
		return recordCount;
	}

	public void runRoutine() {
		while (!this.executorService.isShutdown()) {
			Long dumpTs = this.exportLogsToCsv();
			WmicIngest.log.info("Completed dump with Timestamp {} ", dumpTs);

			try {
				Long records = this.parseCsvAndIngest(dumpTs);
				WmicIngest.log.info("Ingest {} records complete, waiting 10 seconds...", records);
				Thread.sleep(15000);
			} catch (Exception e) {
				WmicIngest.log.error("Failed to ingest CSV... {}", e);

			}
		}
	}

}
