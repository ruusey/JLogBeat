package com.jlogbeat.ingest.windows;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlogbeat.entity.FirewallLog;
import com.jlogbeat.repo.FirewallEventRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImportThread extends Thread {
	private final transient FirewallEventRepository eventLogRepo;

	public ImportThread(@Autowired FirewallEventRepository eventLogRepo) {
		this.eventLogRepo = eventLogRepo;
		Boolean success = false;
		try {
			//			success = this.helk.deleteIndex(WmicIngest.DESTINATION_INDEX);
			//
			//			if (success) {
			//				this.helk.createIndex(WmicIngest.DESTINATION_INDEX, "_doc");
			//				log.info("Create winlogbeat test!");
			//			}
		} catch (Exception e) {
			ImportThread.log.error(">>> Failed to initialize HELK firewall import {}", e.getMessage());
		}
	}

	@Override
	public void run() {
		File in = null;

		ObjectMapper mapper = new ObjectMapper();

		Integer linesRead = 0;
		Long start = Instant.now().toEpochMilli();
		while (!Thread.interrupted()) {
			in = new File(WmicIngest.FIREWALL_LOG);

			LineIterator it = null;
			try {
				it = FileUtils.lineIterator(in, "UTF-8");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
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
					tuple.setAction(lineSplit[2]);
					tuple.setProtocol(lineSplit[3]);
					tuple.setSourceIp(lineSplit[4]);
					tuple.setDestinationIp(lineSplit[5]);
					tuple.setSourcePort(Integer.parseInt(lineSplit[6].equals("-") ? "0" : lineSplit[6]));
					tuple.setDestinationPort(Integer.parseInt(lineSplit[7].equals("-") ? "0" : lineSplit[7]));
					SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

					java.util.Date date = isoFormat.parse(tsString);
					tuple.setTimestamp(new Timestamp(date.getTime()));

					linesRead++;
					ImportThread.log.info("Adding FireWall Event {}", tuple);
					ImportThread.this.eventLogRepo.save(tuple);

				}catch(Exception e) {
					ImportThread.log.error("Failed {}", e);
					continue;
				}
			}
			try {
				// it.close();
				FileWriter fw = new FileWriter(in, false);
				fw.flush();
				fw.close();

				Thread.sleep(20000);
			} catch (Exception e) {
				ImportThread.log.error("Failed {}", e);

			}


		}


	}

}