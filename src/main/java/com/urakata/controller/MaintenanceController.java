package com.urakata.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urakata.dto.MaintenanceCompleteRequestDto;
import com.urakata.dto.MaintenanceTaskDto;
import com.urakata.service.IMaintenanceService;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

	@Autowired
	private IMaintenanceService maintenanceService;

	@GetMapping("/tasks")
	public ResponseEntity<?> getTasks(@RequestParam String projectName) {

		ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		Optional<List<MaintenanceTaskDto>> result = Optional.empty();
		result = maintenanceService.getTasksForProject(projectName);
		if (result.isPresent()) {
			response = new ResponseEntity<>(result.get(), HttpStatus.OK);
		} else {
			response = new ResponseEntity<>("{\"message\" : \"No Record Found}", HttpStatus.BAD_REQUEST);
		}

		// returning response message
		return response;
	}

	@PutMapping("/complete")
	public ResponseEntity<?> markComplete(@RequestBody MaintenanceCompleteRequestDto request) {
		ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		Map<String, String> errors = new HashMap<>();
		boolean isValid = true;

		if (request.getProjectName() == null || request.getProjectName().trim().isEmpty()) {
			isValid = false;
			errors.put("projectName", "Project name must not be blank or null.");
		}

		if (request.getTaskName() == null || request.getTaskName().trim().isEmpty()) {
			isValid = false;
			errors.put("taskName", "Task name must not be blank or null.");
		}

		if (request.getCompletedBy() == null || request.getCompletedBy().trim().isEmpty()) {
			isValid = false;
			errors.put("completedBy", "Completed by must not be blank or null.");
		}

		if (request.getAction() == null || request.getAction().trim().isEmpty()) {
			isValid = false;
			errors.put("action", "Action must not be blank or null.");
		}

		if (!isValid) {
			return ResponseEntity.badRequest().body(errors); // Return early if validation fails
		}

		MaintenanceCompleteRequestDto requestClean = MaintenanceCompleteRequestDto.builder()
				.projectName(request.getProjectName()).taskName(request.getTaskName())
				.completedBy(request.getCompletedBy()).action(request.getAction()).remarks(request.getRemarks())
				.build();

		ResponseEntity<?> result = null;
		result = maintenanceService.markTaskAsComplete(requestClean);
		if (result.getStatusCode().is2xxSuccessful())
			return ResponseEntity.ok("Task marked complete");
		else
			return result;
	}

	@PutMapping("/sheduleMaintenanceTask")
	public ResponseEntity<?> sheduleMaintenanceTask() {
		ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		Map<String, String> errors = new HashMap<>();

		ResponseEntity<?> result = null;
		result = maintenanceService.sheduleMaintenanceTask();
		if (result.getStatusCode().is2xxSuccessful())
			return ResponseEntity.ok("sheduleMaintenanceTask completed successfully");
		else
			return result;
	}
}
