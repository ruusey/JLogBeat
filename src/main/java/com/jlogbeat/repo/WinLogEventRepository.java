package com.jlogbeat.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.jlogbeat.entity.WindowsLog;

@Repository
public interface WinLogEventRepository extends CrudRepository<WindowsLog, Integer> {
	List<WindowsLog> findAllBySourceStrOrderByTimeGeneratedDesc(String sourceStr, Pageable page);

	List<WindowsLog> findAllByEventIdOrderByTimeGeneratedDesc(Integer eventId, Pageable page);

	List<WindowsLog> findAllByOrderByTimeGeneratedDesc(Pageable page);

}
