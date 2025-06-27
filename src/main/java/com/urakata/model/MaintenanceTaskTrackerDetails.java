package com.urakata.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "maintenance_task_tracker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceTaskTrackerDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_name")
	private String projectName;

	@Column(name = "task_name")
	private String taskName;

	@Column(name = "task_created_on")
	private LocalDate taskCreatedOn;

	@Column(name = "task_status")
	private String taskStatus;

	@Column(name = "done_by")
	private String doneBy;

	@Column(name = "done_on")
	private LocalDate doneOn;

	@Column(name = "remarks")
	private String remarks;
}
