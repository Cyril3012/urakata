package com.urakata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.urakata.model.MaintenanceDetails;

@Repository
public interface IMaintenanceDetailsRepository
		extends JpaRepository<MaintenanceDetails, Long>, JpaSpecificationExecutor<MaintenanceDetails> {

	Optional<List<MaintenanceDetails>> findByProjectNameAndStatus(String projectName, String string);

	Optional<MaintenanceDetails> findByProjectNameAndTaskNameAndStatus(String projectName, String taskName,
			String string);

	Optional<List<MaintenanceDetails>> findByStatus(String string);

}
