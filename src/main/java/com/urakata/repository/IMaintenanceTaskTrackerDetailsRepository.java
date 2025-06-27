package com.urakata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.urakata.model.MaintenanceTaskTrackerDetails;

@Repository
public interface IMaintenanceTaskTrackerDetailsRepository extends JpaRepository<MaintenanceTaskTrackerDetails, Long>,
		JpaSpecificationExecutor<MaintenanceTaskTrackerDetails> {

}
