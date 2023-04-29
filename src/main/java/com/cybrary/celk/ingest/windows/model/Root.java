package com.cybrary.celk.ingest.windows.model;

import java.util.ArrayList;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Root{
	@JsonProperty("Id")
	public int id;
	@JsonProperty("Version")
	public int version;
	@JsonProperty("Qualifiers")
	public int qualifiers;
	@JsonProperty("Level")
	public int level;
	@JsonProperty("Task")
	public int task;
	@JsonProperty("Opcode")
	public int opcode;
	@JsonProperty("Keywords")
	public long keywords;
	@JsonProperty("RecordId")
	public int recordId;
	@JsonProperty("ProviderName")
	public String providerName;
	@JsonProperty("ProviderId")
	public String providerId;
	@JsonProperty("LogName")
	public String logName;
	@JsonProperty("ProcessId")
	public int processId;
	@JsonProperty("ThreadId")
	public int threadId;
	@JsonProperty("MachineName")
	public String machineName;
	@JsonProperty("UserId")
	public Object userId;
	@JsonProperty("TimeCreated")
	public Date timeCreated;
	@JsonProperty("ActivityId")
	public Object activityId;
	@JsonProperty("RelatedActivityId")
	public Object relatedActivityId;
	@JsonProperty("ContainerLog")
	public String containerLog;
	@JsonProperty("MatchedQueryIds")
	public ArrayList<Object> matchedQueryIds;
	@JsonProperty("Bookmark")
	public Bookmark bookmark;
	@JsonProperty("LevelDisplayName")
	public String levelDisplayName;
	@JsonProperty("OpcodeDisplayName")
	public Object opcodeDisplayName;
	@JsonProperty("TaskDisplayName")
	public Object taskDisplayName;
	@JsonProperty("KeywordsDisplayNames")
	public ArrayList<String> keywordsDisplayNames;
	@JsonProperty("Properties")
	public ArrayList<Property> properties;
	@JsonProperty("Message")
	public String message;
}