package com.jlogbeat.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class BlugeneLog implements Serializable {

	private static final long serialVersionUID = 4227622987125326694L;

	private Long logId;
	private String date;
	private String machineName;
	private String dateTime;
	private String code;
	private String space;
	private String level;
	private String log;

}
