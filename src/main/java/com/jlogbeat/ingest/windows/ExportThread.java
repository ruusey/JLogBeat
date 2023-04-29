package com.jlogbeat.ingest.windows;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ExportThread extends Thread {
	private String command;
	public Boolean complete = false;

	public ExportThread(String command) {
		this.command = command;
	}

	@Override
	public void run() {
		try {
			boolean success = this.executeExport(this.command);
			if (success) {
				this.complete = success;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean executeExport(String command) throws Exception {
		Process powerShellProcess = Runtime.getRuntime().exec(command);

		String line;
		log.info("Running export logs command [{}]", command);
		BufferedReader stdout = new BufferedReader(new InputStreamReader(powerShellProcess.getInputStream()));
		while ((line = stdout.readLine()) != null) {
			log.info("STDOUT:: " + line);
		}
		stdout.close();
		BufferedReader stderr = new BufferedReader(new InputStreamReader(powerShellProcess.getErrorStream()));
		boolean isError = false;
		while ((line = stderr.readLine()) != null) {
			log.error("STDERR:: " + line);
			isError = true;
		}
		stderr.close();
		log.info("Export logs complete");
		powerShellProcess.getInputStream().close();
		powerShellProcess.getOutputStream().close();
		powerShellProcess.getErrorStream().close();
		powerShellProcess.destroyForcibly();
		powerShellProcess.destroy();
		if (isError) {
			return false;
		} else {
			return true;
		}
	}
}
