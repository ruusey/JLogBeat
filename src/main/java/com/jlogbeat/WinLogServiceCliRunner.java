package com.jlogbeat;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WinLogServiceCliRunner implements CommandLineRunner {


	@Override
	public void run(String... args) throws Exception {
		WinLogServiceCliRunner.log.info("Initializing WinLog ingest");
		if ((System.getenv("EXPORT_DIR") == null) || (System.getenv("DB_HOST") == null)
				|| (System.getenv("DB_PASS") == null)) {
			WinLogServiceCliRunner.log.error(
					"Please define the following Environment Variables before running JLogBeat\n EXPORT_DIR\nDB_HOST\nDB_PASS");
			System.exit(-1);
		}
	}

}
