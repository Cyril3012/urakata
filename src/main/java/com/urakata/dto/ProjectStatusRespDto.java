package com.urakata.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStatusRespDto {
	private String serverName;
	private String serverIp;
	private float memoryUsagePercentage;
	private float cpuUsagePercentage;
	private float ramUsagePercentage;
	private List<ServerPort> serverPortList;
	private List<String> logFileDownload;

}
