package com.urakata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaintenanceCompleteRequestDto {
	private String projectName;
	private String taskName;
	private String completedBy;
	private String action;
	private String remarks;
}
