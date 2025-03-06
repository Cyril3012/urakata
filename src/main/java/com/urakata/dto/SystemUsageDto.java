package com.urakata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemUsageDto {
	private String serverName;
	private String ip;
	private long memoryUsagePercentage;
	private long cpuUsagePercentage;
	private long ramUsagePercentage;
}
