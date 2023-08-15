package com.jlogbeat.entity;


import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "win_log_firewall")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class FirewallLog extends TemporalEntity {
	@Id
	@GeneratedValue
	private Integer id;
	private Timestamp timestamp;
	private String action;
	private String protocol;
	private String sourceIp;
	private String destinationIp;
	private Integer sourcePort;
	private Integer destinationPort;
	private Long size;
	private String tcpFlags;
	private String tcpSyn;
	private String tcAck;
	private String tcpWin;
	private String icmpType;
	private String icmpCode;
	private String info;
	private String path;


}
