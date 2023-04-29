package com.jlogbeat.ingest.windows;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlogbeat.ingest.windows.model.FirewallLog;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImportThread extends Thread {

	public ImportThread() {
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
		RandomAccessFile in = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			in = new RandomAccessFile(WmicIngest.FIREWALL_LOG, "r");
			String line;
			Integer linesRead = 0;
			Long start = Instant.now().toEpochMilli();
			while (true) {
				if ((line = in.readLine()) != null) {
					String[] lineSplit = line.split(" ");
					try {
						FirewallLog tuple = new FirewallLog();
						String tsString = lineSplit[0] + "T" + lineSplit[1];
						tuple.setProtocol("Ru-Test");
						tuple.setSourceIp(lineSplit[4]);
						tuple.setDestinationIp(lineSplit[5]);
						tuple.setSourcePort(Integer.parseInt(lineSplit[6].equals("-") ? "0" : lineSplit[6]));
						tuple.setDestinationPort(Integer.parseInt(lineSplit[7].equals("-") ? "0" : lineSplit[7]));
						LocalDateTime dateTime = LocalDateTime.parse(tsString);
						tuple.setTime(tsString);

						linesRead++;
						//System.out.println(mapper.writeValueAsString(tuple));
					} catch (Exception e) {
						ImportThread.log.error("failed parsing Firewall Log line [{}]  {}", line, e.getMessage());
					}
				} else {
					ImportThread.log.info("Ingested {} firewall logs in {}ms", linesRead, (Instant.now().toEpochMilli() - start));
					start = Instant.now().toEpochMilli();
					linesRead = 0;
					Thread.sleep(5000);
				}
			}
		} catch (Exception e) {
			ImportThread.log.error("Failed running Firewall Log import [{}]  {}", e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

}