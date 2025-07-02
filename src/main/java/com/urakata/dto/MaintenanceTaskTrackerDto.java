package com.urakata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaintenanceTaskTrackerDto {

	private String projectName;
	private String taskName;
	private String taskCreatedOn;
	private String taskStatus;
	private String doneBy;
	private String doneOn;
	private String remarks;
}
