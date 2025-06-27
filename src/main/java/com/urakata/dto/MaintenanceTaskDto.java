package com.urakata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaintenanceTaskDto {
	private String projectName;
	private String taskName;
	private String frequency; // e.g., "30 Days"
	private String lastDone;
	private String nextDue;
	private String status; // PENDING, DONE, NA
	private String completedOn;
	private String completedBy;
	private String remarks;
}
