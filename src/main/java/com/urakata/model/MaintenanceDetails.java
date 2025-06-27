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
@Table(name = "maintenance_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_name")
	private String projectName;

	@Column(name = "task_name")
	private String taskName;

	@Column(name = "frequency")
	private String frequency;

	@Column(name = "effective_date")
	private LocalDate effectiveDate;

	@Column(name = "last_done")
	private LocalDate lastDone;

	@Column(name = "next_due")
	private LocalDate nextDue;

	@Column(name = "done_by")
	private String doneBy;

	@Column(name = "done_on")
	private LocalDate doneOn;

	@Column(name = "task_status")
	private String taskStatus;

	@Column(name = "remarks")
	private String remarks;

	@Column(name = "created_on")
	private LocalDate createdOn;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_on")
	private LocalDate updatedOn;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "status")
	private String status; // ACTIVE, INACTIVE, etc.

}
