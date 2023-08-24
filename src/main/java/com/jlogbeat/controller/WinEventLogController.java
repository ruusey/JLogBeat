package com.jlogbeat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jlogbeat.entity.FirewallLog;
import com.jlogbeat.entity.WindowsLog;
import com.jlogbeat.service.SearchService;
import com.jlogbeat.service.WinLogService;
import com.jlogbeat.util.ApiUtils;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/event")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class WinEventLogController {
	private final transient WinLogService winLogService;
	private final transient SearchService searchService;

	public WinEventLogController(@Autowired WinLogService winLogService, @Autowired SearchService searchService) {
		this.winLogService = winLogService;
		this.searchService = searchService;
	}

	@ApiOperation(value = "Perform Sample Blugene Log search", response = FirewallLog[].class)
	@RequestMapping(value = "/search/{query}", method = RequestMethod.GET, produces = { "application/json" })
	public ResponseEntity<?> getFirewallLogs(@PathVariable String query) {
		ResponseEntity<?> res = null;
		try {
			res = ApiUtils.buildSuccess(this.searchService.suggestLogs(query, 1000));
		} catch (Exception e) {
			WinEventLogController.log.error("Unable to get Blugene Logs", e);
			res = ApiUtils.buildError("Unable to get Blugene Logs");
		}
		return res;
	}

	@ApiOperation(value = "Get paginated event history", response = WindowsLog[].class)
	@RequestMapping(method = RequestMethod.GET, produces = { "application/json" })
	public ResponseEntity<?> getWinLogs(@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "10") Integer size) {
		ResponseEntity<?> res = null;
		try {
			res = ApiUtils.buildSuccess(this.winLogService.getLogs(page, size));
		} catch (Exception e) {
			WinEventLogController.log.error("Unable to get event history", e);
			res = ApiUtils.buildError("Unable to get event history");
		}
		return res;
	}

	@ApiOperation(value = "Get paginated event history", response = FirewallLog[].class)
	@RequestMapping(value = "/firewall", method = RequestMethod.GET, produces = { "application/json" })
	public ResponseEntity<?> getFirewallLogs(@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "10") Integer size) {
		ResponseEntity<?> res = null;
		try {
			res = ApiUtils.buildSuccess(this.winLogService.getFirewallLogs(page, size));
		} catch (Exception e) {
			WinEventLogController.log.error("Unable to get event history", e);
			res = ApiUtils.buildError("Unable to get event history");
		}
		return res;
	}

	@ApiOperation(value = "Get paginated event history", response = FirewallLog[].class)
	@RequestMapping(value = "/firewall/{query}", method = RequestMethod.GET, produces = { "application/json" })
	public ResponseEntity<?> getFirewallLogs(@PathVariable String query,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "10") Integer size) {
		ResponseEntity<?> res = null;
		try {
			if (query.contains(".") && (query.contains("127.0.0") || query.contains("192.168"))) {
				res = ApiUtils.buildSuccess(this.winLogService.getFirewallLogsBySip(query, page, size));

			} else if (query.contains(".")) {
				res = ApiUtils.buildSuccess(this.winLogService.getFirewallLogsByDip(query, page, size));

			} else {
				res = ApiUtils.buildSuccess(this.winLogService.getFirewallLogsByProtocol(query, page, size));

			}
			// res = ApiUtils.buildSuccess(this.winLogService.getLogs(page, size));
		} catch (Exception e) {
			WinEventLogController.log.error("Unable to get event history", e);
			res = ApiUtils.buildError("Unable to get event history");
		}
		return res;
	}

	@ApiOperation(value = "Get paginated event history by source", response = WindowsLog[].class)
	@RequestMapping(value = "/source/{source}", method = RequestMethod.GET, produces = { "application/json" })
	public ResponseEntity<?> getEventLogHistoryBySource(@PathVariable String source,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "10") Integer size) {
		ResponseEntity<?> res = null;
		try {
			res = ApiUtils.buildSuccess(this.winLogService.getLogsBySource(source, page, size));
		} catch (Exception e) {
			WinEventLogController.log.error("Unable to get event history by source", e);
			res = ApiUtils.buildError("Unable to get event history by source");
		}
		return res;
	}

	@ApiOperation(value = "Get Event Log by Its DB ID", response = WindowsLog.class)
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = { "application/json" })
	public ResponseEntity<?> getLogByLogId(@PathVariable("id") Integer id) {
		ResponseEntity<?> res = null;
		try {
			res = ApiUtils.buildSuccess(this.winLogService.getLogById(id));
		} catch (Exception e) {
			WinEventLogController.log.error("Failed to get log by ID " + id, e);
			res = ApiUtils.buildError("Failed to get log by ID " + id);
		}
		return res;
	}


}
