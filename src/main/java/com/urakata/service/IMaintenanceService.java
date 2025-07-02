package com.urakata.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.urakata.dto.MaintenanceCompleteRequestDto;
import com.urakata.dto.MaintenanceTaskDto;

import jakarta.servlet.http.HttpServletResponse;

public interface IMaintenanceService {

	Optional<List<MaintenanceTaskDto>> getTasksForProject(String projectName);

	ResponseEntity<?> markTaskAsComplete(MaintenanceCompleteRequestDto requestClean);

	ResponseEntity<?> sheduleMaintenanceTask();

	void exportFilteredTasksToExcel(String projectName, int year, int month, HttpServletResponse response)
			throws IOException;

}
