package com.urakata.service;

import java.io.IOException;
import java.util.List;

import com.urakata.dto.ProjectStatusRespDto;
import com.urakata.dto.ServerPortStatusRespDto;
import com.urakata.dto.SystemUsageDto;

public interface IPortMonitoringService {

	List<ServerPortStatusRespDto> getServerPortStatuses();

	ProjectStatusRespDto getProjectStatusAsJson(String projectName);

	List<SystemUsageDto> getServerUtilization();

	String executeDownload(String serverName, String logName) throws IOException;

	// void exportExcel(HttpServletResponse response) throws IOException;

}
