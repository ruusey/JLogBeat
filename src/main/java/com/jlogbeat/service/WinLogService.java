package com.jlogbeat.service;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.jlogbeat.entity.FirewallLog;
import com.jlogbeat.entity.WindowsLog;
import com.jlogbeat.ingest.windows.WmicIngest;
import com.jlogbeat.repo.FirewallEventRepository;
import com.jlogbeat.repo.WinLogEventRepository;

@Service
public class WinLogService {
	private final transient WmicIngest ingest;
	private final transient ExecutorService executorService;
	private final transient WinLogEventRepository eventLogRepo;
	private final transient FirewallEventRepository firewallRepo;

	public WinLogService(@Autowired WmicIngest ingest, @Autowired ExecutorService executorService,
			@Autowired WinLogEventRepository eventLogRepo, @Autowired FirewallEventRepository firewallRepo) {
		this.ingest = ingest;
		this.executorService = executorService;
		this.eventLogRepo = eventLogRepo;
		this.firewallRepo = firewallRepo;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void beginCollection() {
		Runnable run = () -> {

			try {
				this.ingest.runRoutine();

			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		this.executorService.execute(new Thread(run));
	}

	public List<WindowsLog> getLogs(Integer page, Integer size) {
		return this.eventLogRepo.findAllByOrderByTimeGeneratedDesc(PageRequest.of(page, size));
	}

	public List<WindowsLog> getLogsBySource(String source, Integer page, Integer size){
		return this.eventLogRepo.findAllBySourceStrOrderByTimeGeneratedDesc(source, PageRequest.of(page, size));
	}

	public List<WindowsLog> getLogsByEventId(Integer eventId, Integer page, Integer size) {
		return this.eventLogRepo.findAllByEventIdOrderByTimeGeneratedDesc(eventId, PageRequest.of(page, size));
	}

	public WindowsLog getLogById(Integer id) throws Exception {
		return this.eventLogRepo.findById(id).get();
	}

	public List<FirewallLog> getFirewallLogs(Integer page, Integer size) {
		return this.firewallRepo.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
	}

	public List<FirewallLog> getFirewallLogsBySip(String sourceIp, Integer page, Integer size) {
		return this.firewallRepo.findAllBySourceIpOrderByTimestampDesc(sourceIp, PageRequest.of(page, size));
	}

	public List<FirewallLog> getFirewallLogsByDip(String destinationIp, Integer page, Integer size) {
		return this.firewallRepo.findAllByDestinationIpOrderByTimestampDesc(destinationIp, PageRequest.of(page, size));
	}

	public List<FirewallLog> getFirewallLogsByProtocol(String protocol, Integer page, Integer size) {
		return this.firewallRepo.findAllByProtocolOrderByTimestampDesc(protocol, PageRequest.of(page, size));
	}

}
