package com.urakata.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.urakata.model.MaintenanceTaskTrackerDetails;

@Repository
public interface IMaintenanceTaskTrackerDetailsRepository extends JpaRepository<MaintenanceTaskTrackerDetails, Long>,
		JpaSpecificationExecutor<MaintenanceTaskTrackerDetails> {

	Optional<List<MaintenanceTaskTrackerDetails>> findByProjectNameAndDoneOnBetween(String projectName,
			LocalDate startDate, LocalDate endDate);

	Optional<List<MaintenanceTaskTrackerDetails>> findByProjectName(String projectName);

}
