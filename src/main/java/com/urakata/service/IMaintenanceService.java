package com.urakata.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.urakata.dto.MaintenanceCompleteRequestDto;
import com.urakata.dto.MaintenanceTaskDto;

public interface IMaintenanceService {

	Optional<List<MaintenanceTaskDto>> getTasksForProject(String projectName);

	ResponseEntity<?> markTaskAsComplete(MaintenanceCompleteRequestDto requestClean);

	ResponseEntity<?> sheduleMaintenanceTask();

}
