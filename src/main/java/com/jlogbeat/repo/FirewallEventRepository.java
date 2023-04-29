package com.jlogbeat.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.jlogbeat.entity.FirewallLog;

@Repository
public interface FirewallEventRepository extends CrudRepository<FirewallLog, Integer> {
	List<FirewallLog> findAllBySourceIpOrderByTimestampDesc(String sourceIp, Pageable page);

	List<FirewallLog> findAllByDestinationIpOrderByTimestampDesc(String destinationIp, Pageable page);

	List<FirewallLog> findAllByProtocolOrderByTimestampDesc(String protocol, Pageable page);

	List<FirewallLog> findAllByOrderByTimestampDesc(Pageable page);

}
