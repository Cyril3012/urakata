package com.urakata.service;

import java.io.IOException;
import java.util.List;

import com.urakata.dto.ProjectStatusRespDto;
import com.urakata.dto.ServerPortStatusRespDto;
import com.urakata.dto.SystemUsageDto;

import jakarta.servlet.http.HttpServletResponse;

public interface IPortMonitoringService {

	List<ServerPortStatusRespDto> getServerPortStatuses();

	ProjectStatusRespDto getProjectStatusAsJson(String projectName);

	List<SystemUsageDto> getServerUtilization();

	String executeDownload(String serverName, String logName) throws IOException;

	String executeRemoteCommand(String string, String string2, String string3) throws IOException;

	String executeRemoteCommandWithPem(String string, String string2) throws IOException;

	void exportExcel(HttpServletResponse response) throws IOException;

}
