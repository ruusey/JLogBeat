package com.cybrary;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WinLogServiceCliRunner implements CommandLineRunner {


	@Override
	public void run(String... args) throws Exception {
		WinLogServiceCliRunner.log.info("Initializing WinLog ingest");
		// this.ingest.runRoutine();
	}

}
