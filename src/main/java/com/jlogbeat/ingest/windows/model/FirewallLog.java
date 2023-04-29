package com.jlogbeat.ingest.windows.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirewallLog {
	private String date;
	private String time;
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
