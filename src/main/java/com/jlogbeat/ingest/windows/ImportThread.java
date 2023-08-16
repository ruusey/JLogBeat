package com.jlogbeat.ingest.windows;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jlogbeat.entity.FirewallLog;
import com.jlogbeat.repo.FirewallEventRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImportThread extends Thread {
	private static final transient String HOST = ImportThread.getMachineHost();
	private final transient FirewallEventRepository eventLogRepo;

	public ImportThread(@Autowired FirewallEventRepository eventLogRepo) {
		this.eventLogRepo = eventLogRepo;
	}

	@Override
	public void run() {
		File in = null;

		Integer linesRead = 0;
		while (!Thread.interrupted()) {
			in = new File(WinlogIngest.FIREWALL_LOG);

			LineIterator it = null;
			try {
				it = FileUtils.lineIterator(in, "UTF-8");
			} catch (IOException e) {
				ImportThread.log.error("Failed to create LineIterator for Firewall Log File at {}, Reason: {}",
						WinlogIngest.FIREWALL_LOG, e);
				break;
			}

			while (it.hasNext()) {
				String line = it.nextLine();
				String[] lineSplit = line.split(" ");
				try {
					FirewallLog tuple = new FirewallLog();

					String tsString = lineSplit[0] + " " + lineSplit[1];
					tsString = tsString.trim();
					if ((tsString == null) || tsString.isEmpty() || tsString.isBlank()) {
						continue;
					}
					SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

					java.util.Date date = isoFormat.parse(tsString);
					tuple.setTimestamp(new Timestamp(date.getTime()));

					tuple.setAction(lineSplit[2]);
					tuple.setProtocol(lineSplit[3]);
					tuple.setSourceIp(lineSplit[4]);
					tuple.setDestinationIp(lineSplit[5]);
					tuple.setSourcePort(Integer.parseInt(lineSplit[6].equals("-") ? "0" : lineSplit[6]));
					tuple.setDestinationPort(Integer.parseInt(lineSplit[7].equals("-") ? "0" : lineSplit[7]));
					tuple.setMachineName(ImportThread.HOST);
					linesRead++;
					ImportThread.log.info("Adding Firewall Event {}", tuple);
					this.eventLogRepo.save(tuple);

				}catch(Exception e) {
					ImportThread.log.error("Failed to parse Firewall Event {}", e);
					continue;
				}
			}
			try {
				it.close();
				FileWriter fw = new FileWriter(in, false);
				fw.write("");
				fw.flush();
				fw.close();
				ImportThread.log.info("Ingest {} Firewall Event records complete, waiting 10 minutes...", linesRead);

				Thread.sleep(60000 * 10);
			} catch (Exception e) {
				ImportThread.log.error("Failed {}", e);
			}
		}
	}

	private static String getMachineHost() {
		String hostname = "Unknown";
		try
		{
			final InetAddress addr=InetAddress.getLocalHost();
			hostname = addr.getHostName();
		}catch(Exception e) {
			ImportThread.log.error("Failed to get machine hostname. Reason: {}", e);
		}
		return hostname;
	}
}