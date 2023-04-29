package com.cybrary.celk.ingest.windows.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "win_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WindowsLog extends TemporalEntity {
	@Id
	@GeneratedValue
	private Integer id;
	private Integer eventId;
	private String machineName;
	@Column(name = "data_str", columnDefinition = "TEXT")

	private String dataStr;
	@Column(unique = true)
	private Integer idx;
	private String category;
	private String categoryNumber;
	private String entryType;
	@Column(name = "message", columnDefinition = "TEXT")

	private String message;
	private String sourceStr;
	@Column(name = "replacement_strings", columnDefinition = "TEXT")

	private String replacementStrings;
	private String instanceId;
	private Timestamp timeGenerated;
}
