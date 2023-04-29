package com.cybrary.celk.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.cybrary.celk.ingest.windows.model.WindowsLog;

public interface WinLogEventRepository extends CrudRepository<WindowsLog, Integer> {
	List<WindowsLog> findAllBySourceStrOrderByTimeGeneratedDesc(String sourceStr, Pageable page);

	List<WindowsLog> findAllByEventIdOrderByTimeGeneratedDesc(Integer eventId, Pageable page);

	List<WindowsLog> findAllOrderByTimeGeneratedDesc(Pageable page);

}
