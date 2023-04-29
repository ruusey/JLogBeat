package com.cybrary.celk.service;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.cybrary.celk.ingest.windows.WmicIngest;
import com.cybrary.celk.ingest.windows.model.WindowsLog;
import com.cybrary.celk.repo.WinLogEventRepository;

@Service
public class WinLogService {
	private final transient WmicIngest ingest;
	private final transient ExecutorService executorService;
	private final transient WinLogEventRepository eventLogRepo;

	public WinLogService(@Autowired WmicIngest ingest, @Autowired ExecutorService executorService,
			@Autowired WinLogEventRepository eventLogRepo) {
		this.ingest = ingest;
		this.executorService = executorService;
		this.eventLogRepo = eventLogRepo;
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
		return this.eventLogRepo.findAllOrderByTimeGeneratedDesc(PageRequest.of(page, size));
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



}
