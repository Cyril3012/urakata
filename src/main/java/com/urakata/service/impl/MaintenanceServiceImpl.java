package com.urakata.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.AbstractVerticalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.urakata.dto.MaintenanceCompleteRequestDto;
import com.urakata.dto.MaintenanceTaskDto;
import com.urakata.dto.MaintenanceTaskTrackerDto;
import com.urakata.model.MaintenanceDetails;
import com.urakata.model.MaintenanceTaskTrackerDetails;
import com.urakata.repository.IMaintenanceDetailsRepository;
import com.urakata.repository.IMaintenanceTaskTrackerDetailsRepository;
import com.urakata.service.IMaintenanceService;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class MaintenanceServiceImpl implements IMaintenanceService {

	@Autowired
	IMaintenanceDetailsRepository maintenanceDetailsRepository;

	@Autowired
	private IMaintenanceTaskTrackerDetailsRepository maintenanceTaskTrackerDetailsRepository;

	// GET ALL TASK FOR THE PROJECT
	@Override
	public Optional<List<MaintenanceTaskDto>> getTasksForProject(String projectName) {

		Optional<List<MaintenanceTaskDto>> response = Optional.empty();
		Optional<List<MaintenanceDetails>> maintenanceTaskObjList = maintenanceDetailsRepository
				.findByProjectNameAndStatus(projectName, "A");

		if (maintenanceTaskObjList.isPresent()) {

			List<MaintenanceDetails> list = maintenanceTaskObjList.get();

			List<MaintenanceDetails> sortedList = list.stream()
					.sorted(Comparator.comparingInt((MaintenanceDetails md) -> extractDays(md.getFrequency()))
							.thenComparing(MaintenanceDetails::getTaskName, String.CASE_INSENSITIVE_ORDER))
					.collect(Collectors.toList());
			List<MaintenanceTaskDto> dtoList = new ArrayList<>();

			for (MaintenanceDetails existingTask : sortedList) {

				MaintenanceTaskDto dto = new MaintenanceTaskDto();

				dto.setProjectName(existingTask.getProjectName());
				dto.setTaskName(existingTask.getTaskName());
				dto.setFrequency(existingTask.getFrequency());
				dto.setStatus(existingTask.getTaskStatus());
				dto.setCompletedBy(existingTask.getDoneBy());
				dto.setRemarks(existingTask.getRemarks());

				// Safely format dates if not null
				dto.setLastDone(formatDate(existingTask.getLastDone()));
				dto.setNextDue(formatDate(existingTask.getNextDue()));
				dto.setCompletedOn(formatDate(existingTask.getDoneOn()));

				dtoList.add(dto);
			}
			response = Optional.of(dtoList);
		}

		return response;
	}

	// MARK THE TASK AS COMPLETE
	@Override
	public ResponseEntity<?> markTaskAsComplete(MaintenanceCompleteRequestDto requestDto) {

		Optional<MaintenanceDetails> maintenanceTaskObjList = maintenanceDetailsRepository
				.findByProjectNameAndTaskNameAndStatus(requestDto.getProjectName(), requestDto.getTaskName(), "A");

		if (maintenanceTaskObjList.isPresent()) {
			MaintenanceDetails taskObj = maintenanceTaskObjList.get();
			if (taskObj.getTaskStatus().equalsIgnoreCase("PENDING")) {

				taskObj.setTaskStatus("DONE");
				taskObj.setDoneBy(requestDto.getCompletedBy());
				taskObj.setDoneOn(LocalDate.now());
				taskObj.setRemarks(requestDto.getRemarks());
				taskObj.setUpdatedBy(requestDto.getCompletedBy());
				taskObj.setUpdatedOn(LocalDate.now());
				maintenanceDetailsRepository.save(taskObj);
				return ResponseEntity.ok("Task '" + requestDto.getProjectName() + "' marked as completed by "
						+ requestDto.getCompletedBy());
			} else {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Task '" + requestDto.getTaskName()
						+ "' is in stage '" + taskObj.getTaskStatus() + "'" + " : Can't be markes as complete.");
			}

		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task '" + requestDto.getTaskName()
					+ "' not found for project '" + requestDto.getProjectName() + "'");
		}

	}

	@Scheduled(cron = "0 0 10 * * ?") // Every day at 10 AM
	public void autoSheduleMaintenanceTask() {
		sheduleMaintenanceTask(); // Call the actual logic
	}

	// TO TRIGGER SHEDULER MANUALY
	/*
	 * based on next due and frequency the existing data is refreshed as new task
	 * and existing data is logged in seperate table
	 */
	@Override
	public ResponseEntity<?> sheduleMaintenanceTask() {

		Optional<List<MaintenanceDetails>> maintenanceTaskObjList = maintenanceDetailsRepository.findByStatus("A");
		ResponseEntity<?> response = null;
		List<MaintenanceDetails> updatedMaintenanceDetails = new ArrayList<>();
		List<MaintenanceTaskTrackerDetails> newMaintenanceTaskLogrList = new ArrayList<>();
		if (maintenanceTaskObjList.isPresent()) {
			List<MaintenanceDetails> taskListObj = maintenanceTaskObjList.get();
			for (MaintenanceDetails task : taskListObj) {

				if (task.getNextDue().getMonthValue() <= LocalDate.now().getMonthValue()) {
					MaintenanceTaskTrackerDetails newMaintenanceTaskLog = new MaintenanceTaskTrackerDetails();

					newMaintenanceTaskLog.setProjectName(task.getProjectName());
					newMaintenanceTaskLog.setTaskName(task.getTaskName());
					newMaintenanceTaskLog.setTaskCreatedOn(task.getCreatedOn()); // Or task.getCreatedOn() if available
					newMaintenanceTaskLog.setTaskStatus(task.getTaskStatus()); // Or task.getStatus() if applicable
					newMaintenanceTaskLog.setDoneBy(task.getDoneBy()); // Leave null if not yet done
					newMaintenanceTaskLog.setDoneOn(task.getDoneOn()); // Leave null if not yet done
					newMaintenanceTaskLog.setRemarks(task.getRemarks());

					newMaintenanceTaskLogrList.add(newMaintenanceTaskLog);

					// update task details master
					if (task.getDoneOn() == null) {
						task.setLastDone(task.getLastDone());
					} else {
						task.setLastDone(task.getDoneOn());
					}
					task.setCreatedBy("SHEDULER");
					task.setCreatedOn(LocalDate.now());
					task.setDoneBy(null);
					task.setDoneOn(null);
					task.setRemarks(null);
					task.setTaskStatus("PENDING");

					LocalDate oldNextDue = task.getNextDue();
					LocalDate nextDueFinal = LocalDate.now().plusDays(Long.parseLong(task.getFrequency().split(" ")[0]))
							.with(TemporalAdjusters.lastDayOfMonth());
					task.setNextDue(nextDueFinal);

					updatedMaintenanceDetails.add(task);
					response = ResponseEntity.ok("Task '" + task.getProjectName() + "' new task created my sheduler ");
				}

			}

			maintenanceTaskTrackerDetailsRepository.saveAll(newMaintenanceTaskLogrList);
			maintenanceDetailsRepository.saveAll(updatedMaintenanceDetails);
			response = ResponseEntity.ok("Saved records from sheduler");
		} else {
			response = ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task  not found for project");
		}
		return response;
	}

	private String formatDate(LocalDate date) {
		return (date != null) ? date.toString() : null;
	}

	// HELPER METHOD TO EXTRACT DAY FROM STRING
	private int extractDays(String frequency) {
		try {
			if (frequency != null && frequency.matches("\\d+\\s*Days")) {
				return Integer.parseInt(frequency.replaceAll("[^0-9]", ""));
			}
		} catch (NumberFormatException e) {
			// log warning if needed
		}
		return Integer.MAX_VALUE; // put invalid/unknown frequencies at the end
	}

	@Override
	public void exportFilteredTasksToExcel(String projectName, int year, int month, HttpServletResponse response)
			throws IOException {
		// Fetch filtered data
		List<MaintenanceTaskTrackerDto> exportList = new ArrayList<>();
		LocalDate startDate = LocalDate.of(year, month, 1);
		LocalDate endDate = YearMonth.of(year, month).atEndOfMonth(); // e.g., 2025-06-30

		Optional<List<MaintenanceTaskTrackerDetails>> filteredTasksObj = maintenanceTaskTrackerDetailsRepository
				.findByProjectNameAndDoneOnBetween(projectName, startDate, endDate);

//		Optional<List<MaintenanceTaskTrackerDetails>> filteredTasksObj = maintenanceTaskTrackerDetailsRepository
//				.findByProjectName(projectName);
		if (filteredTasksObj.isPresent()) {
			List<MaintenanceTaskTrackerDetails> filteredTasks = filteredTasksObj.get();
			// Convert to DTO
			exportList = filteredTasks.stream().map(task -> {
				MaintenanceTaskTrackerDto dto = new MaintenanceTaskTrackerDto();
				dto.setProjectName(task.getProjectName());
				dto.setTaskName(task.getTaskName());
				dto.setTaskCreatedOn(task.getTaskCreatedOn() != null ? task.getTaskCreatedOn().toString() : "");
				dto.setTaskStatus(task.getTaskStatus());
				dto.setDoneBy(task.getDoneBy());
				dto.setDoneOn(task.getDoneOn() != null ? task.getDoneOn().toString() : "");
				dto.setRemarks(task.getRemarks());
				return dto;
			}).toList();
		}
		String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		EasyExcel.write(response.getOutputStream(), MaintenanceTaskTrackerDto.class)
				.registerWriteHandler(getCustomExcelStyle()) // your custom header/content style
				.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()) // auto column width
				.sheet("Maintenance Tasks_" + year + "_" + monthName).doWrite(exportList);

	}

	private AbstractVerticalCellStyleStrategy getCustomExcelStyle() {
		return new AbstractVerticalCellStyleStrategy() {
			@Override
			protected WriteCellStyle headCellStyle(CellWriteHandlerContext context) {
				WriteCellStyle style = new WriteCellStyle();
				style.setHorizontalAlignment(HorizontalAlignment.LEFT);
				style.setVerticalAlignment(VerticalAlignment.CENTER);
				style.setWrapped(false); // Prevent wrapping
				style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());

				WriteFont font = new WriteFont();
				font.setBold(true);
				font.setColor(IndexedColors.WHITE.getIndex());
				style.setWriteFont(font);
				return style;
			}

			@Override
			protected WriteCellStyle contentCellStyle(CellWriteHandlerContext context) {
				WriteCellStyle style = new WriteCellStyle();
				style.setHorizontalAlignment(HorizontalAlignment.LEFT);
				style.setVerticalAlignment(VerticalAlignment.CENTER);
				style.setWrapped(true);

				WriteFont font = new WriteFont();
				font.setBold(false);
				font.setColor(IndexedColors.BLACK.getIndex());
				style.setWriteFont(font);
				return style;
			}
		};
	}

}
