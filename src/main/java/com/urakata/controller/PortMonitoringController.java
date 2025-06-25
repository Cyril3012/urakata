package com.urakata.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.urakata.dto.ProjectStatusRespDto;
import com.urakata.dto.ServerPortStatusRespDto;
import com.urakata.dto.SystemUsageDto;
import com.urakata.service.IPortMonitoringService;

@Controller
@RequestMapping("/monitor")
public class PortMonitoringController {

	@Autowired
	private IPortMonitoringService portMonitoringService;

	/* API TO GET THE PORT STATUS DETAILS */
	@GetMapping("/status")
	public String getServerPortStatuses(Model model) {
		// portMonitoringService.monitorPorts();
		List<ServerPortStatusRespDto> servers = portMonitoringService.getServerPortStatuses();
		System.out.println("Servers: " + servers); // Log the data
		model.addAttribute("servers", servers);

		return "monitor";
	}

	// New endpoint to serve JSON data
	@GetMapping("/status-data")
	@ResponseBody
	public List<ServerPortStatusRespDto> getServerPortStatusesAsJson() {
		return portMonitoringService.getServerPortStatuses();
	}

	@GetMapping("/project-data")
	@ResponseBody
	public ProjectStatusRespDto getProjectStatusAsJson(String projectName) {

		return portMonitoringService.getProjectStatusAsJson(projectName);
	}

	@GetMapping("/system-usage")
	@ResponseBody
	public List<SystemUsageDto> getSystemUsage() {
		List<SystemUsageDto> serversUtil = portMonitoringService.getServerUtilization();

		return serversUtil;
	}

	@GetMapping("/simple")
	public String simple(Model model) throws IOException {

		List<SystemUsageDto> serversUtil = portMonitoringService.getServerUtilization();
		System.out.println("Servers: " + serversUtil); // Log the data
		model.addAttribute("server", serversUtil);
		model.addAttribute("message", "This is a test");
		return "simple"; // Refers to 'simple.html' in the templates folder

	}

	@GetMapping("/projectStatus")
	public String showProjectStatusPage(@RequestParam(name = "projectName", required = false) String projectName,
			Model model) {
		model.addAttribute("projectName", projectName);
		return "projcetDetails"; // This should return the correct HTML file
	}

	// DOWNLOAD SERVER LOG API
	@GetMapping("/downloadLogApi")
	public ResponseEntity<String> downloadLogFile(@RequestParam String serverName, @RequestParam String logName)
			throws IOException {
		String success = portMonitoringService.executeDownload(serverName, logName);

		if (!success.toLowerCase().contains("fail")) {
			return ResponseEntity.ok(success);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(success);
		}
	}

//	@GetMapping("/download")
//	public void downloadExcel(HttpServletResponse response) throws IOException {
//		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//		response.setHeader("Content-Disposition", "attachment; filename=DynamicExcel.xlsx");
//
//		portMonitoringService.exportExcel(response);
//	}
}
