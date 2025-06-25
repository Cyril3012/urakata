package com.urakata.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.urakata.dto.ProjectStatusRespDto;
import com.urakata.dto.ServerPort;
import com.urakata.dto.ServerPortStatusRespDto;
import com.urakata.dto.SystemUsageDto;
import com.urakata.model.ServerDetails;
import com.urakata.model.ServerDetails.FileSystemDto;
import com.urakata.model.ServerDetails.ServiceDto;
import com.urakata.repository.IServerDetailsRepository;
import com.urakata.service.IPortMonitoringService;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Service
public class PortMonitoringServiceImpl implements IPortMonitoringService {

	@Autowired
	IServerDetailsRepository serverDetailsRepository;

	private final List<ServerPortStatusRespDto> tempServerPortStatuses = new ArrayList<>();
	private final List<ServerPortStatusRespDto> serverPortStatuses = new ArrayList<>();

	private final List<SystemUsageDto> tempServerUtilization = new ArrayList<>();
	private final List<SystemUsageDto> serverUtilization = new ArrayList<>();

	@Scheduled(fixedRate = 60000)
	public void checkUtlization() throws IOException {
		tempServerUtilization.clear();
		List<ServerDetails> allServerDetails = serverDetailsRepository.findAll();
		for (ServerDetails server : allServerDetails) {

			String ip = null;
			String pemFilePath = null;
			String userName = null;
			String password = null;
			long[] utilization = new long[] { 0, 0, 0 };
			// call the system utilization method dynamically
			ip = server.getServerIp();
			userName = server.getUsername();
			pemFilePath = server.getLocalPublicKeyPath();
			if (!(server.getPemFilePath() == null) && !server.getPemFilePath().isEmpty()) {

				pemFilePath = server.getPemFilePath();
				utilization = parseUtilizationResult(executeRemoteCommandWithPem(ip, pemFilePath, userName)); // [memoryUsage,
																												// cpuUtilization,
																												// ramUtilization]

			} else if (server.getUsername() != null && (!server.getUsername().isEmpty())
					&& (server.getPassword() != null) && !server.getPassword().isEmpty()) {
				userName = server.getUsername();
				password = server.getPassword();
				try {
					String sysUsage = executeRemoteCommand(ip, userName, password, "linux");
					if (sysUsage != null && !sysUsage.isBlank()) {
						utilization = parseUtilizationResult(sysUsage);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("Failed to connect to " + ip + ": " + e.getMessage());
				}
				// utilization = getServerUtilization(userName, password, server.getServerIp(),
				// 22);
			} else if (pemFilePath != null) {
				userName = server.getUsername();
				try {
					utilization = parseUtilizationResult(executeRemoteCommandWithPem(ip, pemFilePath, userName));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("Failed to connect to " + ip + ": " + e.getMessage());
				}
			}
//			long memoryUsage = systemUsages[0];
//			long cpuUtilization = systemUsages[1];
//			long ramUtilization = systemUsages[2];

			// uncomment this to send
			tempServerUtilization.add(new SystemUsageDto(server.getServerName(), server.getServerIp(), utilization[0],
					utilization[1], utilization[2]));
		}
		synchronized (this) {
			serverUtilization.clear();
			serverUtilization.addAll(tempServerUtilization);
		}
	}

	public List<SystemUsageDto> getServerUtilization() {
		// checkUtlization();
		return serverUtilization;
	}

	// Scheduled task to monitor ports
	@Scheduled(fixedRate = 10000) // Every 10 seconds
	public void monitorPorts() {
		List<ServerDetails> allServerDetails = serverDetailsRepository.findAll();
		tempServerPortStatuses.clear(); // Clear previous statuses
		for (ServerDetails serverDetail : allServerDetails) {
			ServerPortStatusRespDto porjectServiceStatus = new ServerPortStatusRespDto();

			porjectServiceStatus.setServerIp(serverDetail.getServerIp());
			porjectServiceStatus.setServerName(serverDetail.getServerName());

			// Ensure the list is initialized before adding elements
			if (porjectServiceStatus.getServerPortList() == null) {
				porjectServiceStatus.setServerPortList(new ArrayList<>()); // Initialize list
			}
			if (serverDetail.getServices() != null || !serverDetail.getServices().isEmpty()) {
				for (ServiceDto ports : serverDetail.getServices()) {
					String status = checkPortStatus(serverDetail.getServerIp(), ports.getPort());
					ServerPort serverPortStatus = new ServerPort();
					serverPortStatus.setServiceName(ports.getName());
					serverPortStatus.setPort(ports.getPort());
					serverPortStatus.setStatus(status);

					// Add serverPortStatus to the list
					porjectServiceStatus.getServerPortList().add(serverPortStatus);
				}

				tempServerPortStatuses.add(porjectServiceStatus);
			}
		}
		serverPortStatuses.clear();
		serverPortStatuses.addAll(tempServerPortStatuses);
	}

	private String checkPortStatus(String serverIp, int port) {
		try (Socket socket = new Socket(serverIp, port)) {
			return "UP";
		} catch (Exception e) {
			return "DOWN";
		}
	}

	public List<ServerPortStatusRespDto> getServerPortStatuses() {
		// monitorPorts();
		return serverPortStatuses;
	}

	// GET PROJECT DETAILS

	public ProjectStatusRespDto getProjectStatusAsJson(String projectName) {

		ProjectStatusRespDto projectStatusRespObj = new ProjectStatusRespDto();
		Optional<ServerDetails> serverObj = serverDetailsRepository.findByServerName(projectName);
		if (serverObj.isPresent()) {

			projectStatusRespObj.setServerName(serverObj.get().getServerName());
			projectStatusRespObj.setServerIp(serverObj.get().getServerIp());
			// set service details

			for (ServerPortStatusRespDto currentServiceStatus : serverPortStatuses) {
				if (currentServiceStatus.getServerName().equals(projectName)) {
					projectStatusRespObj.setServerPortList(currentServiceStatus.getServerPortList());
				}
			}
			// set log file details

			List<String> allLogsOfServerList = new ArrayList<>();

			for (FileSystemDto logFileDetails : serverObj.get().getFileSystem()) {
				allLogsOfServerList.add(logFileDetails.getName());
			}
			projectStatusRespObj.setLogFileDownload(allLogsOfServerList);

			// set system utilization

			for (SystemUsageDto currentUtiliationStatus : serverUtilization) {
				if (currentUtiliationStatus.getServerName().equals(projectName)) {

					projectStatusRespObj.setMemoryUsagePercentage(currentUtiliationStatus.getMemoryUsagePercentage());
					projectStatusRespObj.setCpuUsagePercentage(currentUtiliationStatus.getCpuUsagePercentage());
					projectStatusRespObj.setRamUsagePercentage(currentUtiliationStatus.getRamUsagePercentage());
				}
			}
		} else {
			projectStatusRespObj.setServerName("Server not found");
			projectStatusRespObj.setServerIp("Unreachable IP Address");
		}

		return projectStatusRespObj;
	}

	public static long[] getServerUtilization(String username, String password, String host, int port) {
		JSch jsch = new JSch();
		long[] utilization = new long[3]; // memoryUsage, cpuUtilization, ramUtilization

		try {
			Session session = jsch.getSession(username, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");

			System.out.println("Connecting to " + host + "...");
			session.connect();

			// Memory and RAM Usage using 'free'
			String memCommand = "free -m | awk 'NR==2{printf \"%s %s\", $2, $3}'";
			String memOutput = executeCommand(session, memCommand);
			if (memOutput != null) {
				String[] memParts = memOutput.trim().split(" ");
				long totalMem = Long.parseLong(memParts[0]); // in MB
				long usedMem = Long.parseLong(memParts[1]); // in MB
				utilization[0] = usedMem; // memory usage
				utilization[2] = (usedMem * 100) / totalMem; // RAM usage %
			}

			// CPU Utilization using 'top'
			String cpuCommand = "top -bn1 | grep '%Cpu' | awk '{print 100 - $8}'";
			String cpuOutput = executeCommand(session, cpuCommand);
			if (cpuOutput != null) {
				utilization[1] = (long) Float.parseFloat(cpuOutput.trim());
			}

			session.disconnect();
			System.out.println("Disconnected from " + host);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return utilization;
	}

	private static String executeCommand(Session session, String command) throws Exception {
		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		channel.setCommand(command);
		channel.setInputStream(null);
		InputStream inputStream = channel.getInputStream();
		channel.connect();

		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		}

		channel.disconnect();
		return output.toString().trim();
	}
	// DOWNLOAD LOG

	public String executeDownload(String serverName, String logName) throws IOException {

		Optional<ServerDetails> serverObj = serverDetailsRepository.findByServerName(serverName);
		String ip;
		String pemFilePath = null;
		String username = null;
		String password = null;
		String remoteFilePath = null;
		String localFilePath = null;
		String returnMessage = "Failed to download log file : " + logName;
		boolean success = false;
		if (serverObj.isPresent()) {

			ip = serverObj.get().getServerIp();

			List<FileSystemDto> fileSysDto = serverObj.get().getFileSystem();

			for (FileSystemDto filePath : fileSysDto) {
				if (filePath.getName().equalsIgnoreCase(logName)) {
					localFilePath = filePath.getBackupPath();
					remoteFilePath = filePath.getLogpath();
				}
			}
			if (!(localFilePath == null || remoteFilePath == null)) {
				// USING PEM FILE
				pemFilePath = serverObj.get().getLocalPublicKeyPath();
				if (pemFilePath != null) {
					username = serverObj.get().getUsername();
					success = downloadFileWithPemFile(ip, pemFilePath, remoteFilePath, localFilePath, username);
				} else if (!(serverObj.get().getPemFilePath() == null) && !serverObj.get().getPemFilePath().isEmpty()) {
					username = serverObj.get().getUsername();
					pemFilePath = serverObj.get().getPemFilePath();
					success = downloadFileWithPemFile(ip, pemFilePath, remoteFilePath, localFilePath, username);
				}

				// USING CREDENTIALS

				else if (serverObj.get().getUsername() != null && (!serverObj.get().getUsername().isEmpty())
						&& (serverObj.get().getPassword() != null) && !serverObj.get().getPassword().isEmpty()) {
					username = serverObj.get().getUsername();
					password = serverObj.get().getPassword();
					success = downloadFilesWithPassword(ip, username, password, remoteFilePath, localFilePath);
				} else {
					returnMessage = "Failed to get credentials";
				}
			} else {
				returnMessage = "Failed to get log path details.";
			}
		} else {
			returnMessage = "Server not found in database";
		}
		if (success) {
			returnMessage = "Log file downloaded successfully in " + localFilePath;
		} else {
			returnMessage = ("Failed to download log file.");
		}
		return returnMessage;
	}

	// working
	public boolean downloadFileWithPemFile(String ip, String pemFilePath, String remoteFilePath, String localFilePath,
			String userName) throws IOException {
		SSHClient ssh = new SSHClient();
		ssh.addHostKeyVerifier(new PromiscuousVerifier()); // Disable host key checking

		try {
			System.out.println("Loading private key from: " + pemFilePath);
			ssh.loadKeys(pemFilePath);
			System.out.println("Connecting to server: " + ip);
			ssh.connect(ip);
			System.out.println("Authenticating as user: " + userName);
			ssh.authPublickey(userName);
			System.out.println("Starting SFTP session...");
			try (SFTPClient sftp = ssh.newSFTPClient()) {
				System.out.println("Downloading file: " + remoteFilePath + " to " + localFilePath);
				sftp.get(remoteFilePath, localFilePath);
			}

			System.out.println("File downloaded successfully to: " + localFilePath);
			return true;
		} catch (IOException e) {
			System.err.println("Network error: " + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			System.out.println("Disconnecting from server...");
			ssh.disconnect();
		}
	}

	public boolean downloadFilesWithPassword(String ip, String username, String password, String remoteFilePath,
			String localFilePath) throws IOException {
		// Initialize SSHClient
		SSHClient ssh = new SSHClient();
		ssh.addHostKeyVerifier(new PromiscuousVerifier()); // Disable host key checking

		try {
			System.out.println("Connecting to server: " + ip);

			// Connect to the server using username and password
			ssh.connect(ip);
			System.out.println("Authenticating as user: " + username);
			ssh.authPassword(username, password); // Authenticate using password

			System.out.println("Starting SFTP session...");
			try (SFTPClient sftp = ssh.newSFTPClient()) {
				System.out.println("Downloading file: " + remoteFilePath + " to " + localFilePath);
				sftp.get(remoteFilePath, localFilePath); // Download the file
			}

			System.out.println("File downloaded successfully to: " + localFilePath);
			return true;
		} catch (IOException e) {
			System.err.println("Network error: " + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			e.printStackTrace();
			return false;
		} finally {
			System.out.println("Disconnecting from server...");
			ssh.disconnect(); // Disconnect from the server
		}
	}

	// server utilization with username and password using ssh connection
	public String executeRemoteCommand(String ip, String username, String password, String os) throws IOException {
		SSHClient ssh = new SSHClient();
		String result = null;
		ssh.addHostKeyVerifier(new PromiscuousVerifier());

		try {
			ssh.connect(ip);
			ssh.authPassword(username, password);
			os = "linux";
			String command;
			if (os.equalsIgnoreCase("linux")) {
				command = "echo 'CPU:' $(top -bn1 | grep 'Cpu(s)' | awk '{print $2 + $4}') '%' "
						+ "&& echo 'Memory:' $(free | awk '/Mem/{printf(\"%.2f\", $3/$2 * 100.0)}') '%' "
						+ "&& echo 'Disk:' $(df --output=pcent / | tail -n 1 | tr -d ' ')";
			} else if (os.equalsIgnoreCase("windows")) {
				command = "wmic cpu get LoadPercentage /value & "
						+ "wmic OS get FreePhysicalMemory,TotalVisibleMemorySize /value & "
						+ "wmic LogicalDisk where DeviceID='C:' get Size,FreeSpace /value";
			} else {
				return "Invalid OS type. Use 'linux' or 'windows'.";
			}

			try (net.schmizz.sshj.connection.channel.direct.Session session = ssh.startSession()) {
				net.schmizz.sshj.connection.channel.direct.Session.Command cmd = session.exec(command);
				result = new String(cmd.getInputStream().readAllBytes());
				// CPU: 5.3 %
				// Memory: 12.85 %
				// Disk: 3%
				if (os.equalsIgnoreCase("windows")) {
					result = result.replaceAll("\\s+", " ").trim();

					// Regular expressions to match each value
					Pattern loadPercentagePattern = Pattern.compile("LoadPercentage=(\\d+)");
					Pattern freePhysicalMemoryPattern = Pattern.compile("FreePhysicalMemory=(\\d+)");
					Pattern totalVisibleMemorySizePattern = Pattern.compile("TotalVisibleMemorySize=(\\d+)");
					Pattern freeSpacePattern = Pattern.compile("FreeSpace=(\\d+)");
					Pattern sizePattern = Pattern.compile("(?<=\\d|\\s)Size=(\\d+)");

					// Extract values using regex patterns
					long loadPercentage = extractValue(loadPercentagePattern, result);
					long freePhysicalMemory = extractValue(freePhysicalMemoryPattern, result);
					long totalVisibleMemorySize = extractValue(totalVisibleMemorySizePattern, result);
					long freeSpace = extractValue(freeSpacePattern, result);
					long size = extractValue(sizePattern, result);
					int memoryPercentage = 0;
					int diskPercentage = 0;
					if (totalVisibleMemorySize != 0) {
						memoryPercentage = (int) (((double) (totalVisibleMemorySize - freePhysicalMemory)
								/ totalVisibleMemorySize) * 100);
					}

					// Calculate disk usage percentage
					if (size != 0 && size >= freeSpace) {
						diskPercentage = (int) (((double) (size - freeSpace) / size) * 100);
					}

					result = "CUP : " + loadPercentage + " % \n" + "Memory : " + memoryPercentage + " % \n" + "Disk : "
							+ diskPercentage + " %";
				}

				System.out.println(result);
				return result;
			}
		} catch (IOException e) {
			// Instead of rethrowing, log and return empty or null
			System.err.println("SSH Connection failed to " + ip + ": " + e.getMessage());
			return "";
		} finally {
			ssh.disconnect();
		}
	}

	public static long extractValue(Pattern pattern, String result) {
		Matcher matcher = pattern.matcher(result);
		if (matcher.find()) {
			return Long.parseLong(matcher.group(1));
		}
		return 0;
	}

	public String executeRemoteCommandWithPem(String ip, String pemFilePath, String user) throws IOException {
		SSHClient ssh = new SSHClient();
		ssh.addHostKeyVerifier(new PromiscuousVerifier()); // Disable host key checking

		try {
			System.out.println("Loading private key from: " + pemFilePath);
			ssh.loadKeys(pemFilePath); // Load the private key for authentication
			System.out.println("Connecting to server: " + ip);
			ssh.connect(ip); // Connect to the remote server
			System.out.println("Authenticating as user: " + user);
			ssh.authPublickey(user);
			String os = "linux"; // Change this based on your target OS
			String command;

			// Command to run depending on the OS
			if (os.equalsIgnoreCase("linux")) {
				command = "echo 'CPU:' $(top -bn1 | grep 'Cpu(s)' | awk '{print $2 + $4}') '%' "
						+ "&& echo 'Memory:' $(free | awk '/Mem/{printf(\"%.2f\", $3/$2 * 100.0)}') '%' "
						+ "&& echo 'Disk:' $(df --output=pcent / | tail -n 1 | tr -d ' ')";
			} else if (os.equalsIgnoreCase("windows")) {
				command = "wmic cpu get LoadPercentage /value & "
						+ "wmic OS get FreePhysicalMemory,TotalVisibleMemorySize /value & "
						+ "wmic LogicalDisk where DeviceID='C:' get Size,FreeSpace /value";
			} else {
				return "Invalid OS type. Use 'linux' or 'windows'.";
			}

			// Execute the command
			try (net.schmizz.sshj.connection.channel.direct.Session session = ssh.startSession()) {
				net.schmizz.sshj.connection.channel.direct.Session.Command cmd = session.exec(command);
				String result = new String(cmd.getInputStream().readAllBytes());

				// If the OS is windows, we will process the result
				if (os.equalsIgnoreCase("windows")) {
					result = result.replaceAll("\\s+", " ").trim();

					// Regular expressions to match each value
					Pattern loadPercentagePattern = Pattern.compile("LoadPercentage=(\\d+)");
					Pattern freePhysicalMemoryPattern = Pattern.compile("FreePhysicalMemory=(\\d+)");
					Pattern totalVisibleMemorySizePattern = Pattern.compile("TotalVisibleMemorySize=(\\d+)");
					Pattern freeSpacePattern = Pattern.compile("FreeSpace=(\\d+)");
					Pattern sizePattern = Pattern.compile("(?<=\\d|\\s)Size=(\\d+)");

					// Extract values using regex patterns
					long loadPercentage = extractValue(loadPercentagePattern, result);
					long freePhysicalMemory = extractValue(freePhysicalMemoryPattern, result);
					long totalVisibleMemorySize = extractValue(totalVisibleMemorySizePattern, result);
					long freeSpace = extractValue(freeSpacePattern, result);
					long size = extractValue(sizePattern, result);

					int memoryPercentage = 0;
					int diskPercentage = 0;

					// Calculate memory usage percentage
					if (totalVisibleMemorySize != 0) {
						memoryPercentage = (int) (((double) (totalVisibleMemorySize - freePhysicalMemory)
								/ totalVisibleMemorySize) * 100);
					}

					// Calculate disk usage percentage
					if (size != 0 && size >= freeSpace) {
						diskPercentage = (int) (((double) (size - freeSpace) / size) * 100);
					}

					result = "CPU : " + loadPercentage + " % \n" + "Memory : " + memoryPercentage + " % \n" + "Disk : "
							+ diskPercentage + " %";
				}

				System.out.println(result);
				return result;
			} catch (IOException e) {
				// Instead of rethrowing, log and return empty or null
				System.err.println("SSH Connection failed to " + ip + ": " + e.getMessage());
				return "";
			}
		} finally {
			ssh.disconnect(); // Make sure to disconnect the SSH client after use
		}
	}

	public static long[] parseUtilizationResult(String result) {
		long[] utilization = new long[3]; // [memoryUsage (Disk %), cpuUtilization (%), ramUtilization (%)]

		try {
			// CPU Usage (%)
			Pattern cpuPattern = Pattern.compile("CPU[:\\s]*([\\d.]+)");
			Matcher cpuMatcher = cpuPattern.matcher(result);
			if (cpuMatcher.find()) {
				double cpu = Double.parseDouble(cpuMatcher.group(1));
				utilization[1] = (long) cpu; // Store CPU usage as long
			}

			// Memory Usage (%)
			Pattern memPattern = Pattern.compile("Memory[:\\s]*([\\d.]+)");
			Matcher memMatcher = memPattern.matcher(result);
			if (memMatcher.find()) {
				double mem = Double.parseDouble(memMatcher.group(1));
				utilization[2] = (long) mem; // Store RAM usage as long
			}

			// Disk Usage (%)
			Pattern diskPattern = Pattern.compile("Disk[:\\s]*([\\d.]+)");
			Matcher diskMatcher = diskPattern.matcher(result);
			if (diskMatcher.find()) {
				double disk = Double.parseDouble(diskMatcher.group(1));
				utilization[0] = (long) disk; // Store disk as memory usage approximation
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return utilization;
	}

	/*
	 * 
	 * // // // SERVER UTILIZATION // public long[] getSystemUsage(String ip, String
	 * pemFilePath) { // long[] usage = new long[3]; // [memoryUsage,
	 * cpuUtilization, ramUtilization] // // try { // // Get Memory Usage //
	 * ProcessBuilder memoryBuilder = new ProcessBuilder("ssh", "-o",
	 * "StrictHostKeyChecking=no", "-i", // pemFilePath, ip, "free -m"); // Process
	 * memoryProcess = memoryBuilder.start(); // BufferedReader memoryReader = new
	 * BufferedReader(new InputStreamReader(memoryProcess.getInputStream())); //
	 * String memoryLine; // long totalMemory = 0; // long usedMemory = 0; // //
	 * while ((memoryLine = memoryReader.readLine()) != null) { // if
	 * (memoryLine.contains("Mem:")) { // String[] stats = memoryLine.split("\\s+");
	 * // totalMemory = Long.parseLong(stats[1]); // Total memory in MB //
	 * usedMemory = Long.parseLong(stats[2]); // Used memory in MB // usage[0] =
	 * (usedMemory * 100) / totalMemory; // Memory usage percentage // } // } //
	 * //// ProcessBuilder cpuBuilder = new ProcessBuilder("ssh", "-o",
	 * "StrictHostKeyChecking=no", "-i", pemFilePath, //// ip,
	 * "grep 'cpu ' /proc/stat"); // ProcessBuilder cpuBuilder = new
	 * ProcessBuilder("ssh", "-o", "StrictHostKeyChecking=no", "-i", pemFilePath, //
	 * ip, "top -bn1 | grep 'Cpu(s)' | awk '{print $2 + $4}'"); // Process
	 * cpuProcess = cpuBuilder.start(); // BufferedReader cpuReader = new
	 * BufferedReader(new InputStreamReader(cpuProcess.getInputStream())); // String
	 * cpuLine = cpuReader.readLine(); // if (cpuLine != null &&
	 * !cpuLine.trim().isEmpty()) { // double cpuUsage =
	 * Double.parseDouble(cpuLine.trim()); // usage[1] = (long) cpuUsage; //
	 * System.out.println("CPU Usage: " + cpuUsage + "%"); // } else { //
	 * System.out.println("Failed to fetch CPU usage."); // } // // // Get RAM
	 * Utilization // ProcessBuilder ramBuilder = new ProcessBuilder("ssh", "-o",
	 * "StrictHostKeyChecking=no", "-i", pemFilePath, // ip, "cat /proc/meminfo");
	 * // Process ramProcess = ramBuilder.start(); // BufferedReader ramReader = new
	 * BufferedReader(new InputStreamReader(ramProcess.getInputStream())); // String
	 * ramLine; // long totalRam = 0; // long freeRam = 0; // // while ((ramLine =
	 * ramReader.readLine()) != null) { // if (ramLine.startsWith("MemTotal:")) { //
	 * totalRam = Long.parseLong(ramLine.split("\\s+")[1]); // Total RAM in KB // }
	 * else if (ramLine.startsWith("MemFree:")) { // freeRam =
	 * Long.parseLong(ramLine.split("\\s+")[1]); // Free RAM in KB // } // } //
	 * usage[2] = ((totalRam - freeRam) * 100) / totalRam; // RAM utilization
	 * percentage // // } catch (Exception e) { // e.printStackTrace(); // } // //
	 * return usage; // }
	 * 
	 * // public static void executeRemoteCommandJsh(String ip, String pemFilePath,
	 * String command) { // JSch jsch = new JSch(); // // try { // // Add the
	 * private key (.pem file) //
	 * jsch.addIdentity("D:\\ProjectGi\\10.Others\\ProjectGi.pem"); // // // Create
	 * a session using the IP and default SSH port (22) // Session session =
	 * jsch.getSession("ubuntu", ip, 22); // Replace 'ubuntu' with your remote
	 * server username //// session.setConfig("PreferredAuthentications",
	 * "publickey"); //// session.setConfig("StrictHostKeyChecking", "no"); //
	 * session.setConfig("PreferredAuthentications",
	 * "publickey,keyboard-interactive,password"); //
	 * session.setConfig("StrictHostKeyChecking", "no"); //
	 * System.out.println("Connecting to " + ip + " using PEM file..."); //
	 * session.connect(); // // System.out.println("Connection established!"); // //
	 * // Open a channel to execute the command // Channel channel =
	 * session.openChannel("exec"); // ((ChannelExec) channel).setCommand(command);
	 * // // // Get the command output // InputStream inputStream =
	 * channel.getInputStream(); // channel.connect(); // //
	 * System.out.println("Command Output:"); // try (BufferedReader reader = new
	 * BufferedReader(new InputStreamReader(inputStream))) { // String line; //
	 * while ((line = reader.readLine()) != null) { // System.out.println(line); //
	 * Print the command output // } // } // // channel.disconnect(); //
	 * session.disconnect(); // System.out.println("Disconnected from " + ip); // //
	 * } catch (JSchException e) { // System.err.println("Connection Error: " +
	 * e.getMessage()); // if (e.getCause() instanceof java.net.ConnectException) {
	 * // System.err //
	 * .println("Unable to reach the server. Please check the server details and network connection."
	 * ); // } // e.printStackTrace(); // } catch (IOException e) { //
	 * System.err.println("IO Error: " + e.getMessage()); // e.printStackTrace(); //
	 * } // }
	 * 
	 * // public static void executeRemoteCommand(String username, String password,
	 * String host, int port, String command) { // JSch jsch = new JSch(); // // try
	 * { // // Set up the session // Session session = jsch.getSession(username,
	 * host, port); // session.setPassword(password); // // // Avoid asking for key
	 * confirmation // session.setConfig("StrictHostKeyChecking", "no"); // //
	 * System.out.println("Connecting to " + host + " on port " + port + "..."); //
	 * session.connect(); // 10 seconds timeout (10000) // //
	 * System.out.println("Connection established!"); // // // Open a channel to
	 * execute the command // Channel channel = session.openChannel("exec"); //
	 * ((ChannelExec) channel).setCommand(command); // // // Get the command output
	 * // InputStream inputStream = channel.getInputStream(); // channel.connect();
	 * // // System.out.println("Command Output:"); // try (BufferedReader reader =
	 * new BufferedReader(new InputStreamReader(inputStream))) { // String line; //
	 * while ((line = reader.readLine()) != null) { // System.out.println(line); //
	 * Print the command output // } // } // // channel.disconnect(); //
	 * session.disconnect(); // System.out.println("Disconnected from " + host); //
	 * // } catch (JSchException e) { // System.err.println("Connection Error: " +
	 * e.getMessage()); // if (e.getCause() instanceof java.net.ConnectException) {
	 * // System.err //
	 * .println("Unable to reach the server. Please check the server details and network connection."
	 * ); // } // e.printStackTrace(); // } catch (IOException e) { //
	 * System.err.println("IO Error: " + e.getMessage()); // e.printStackTrace(); //
	 * } // }
	 */
	/*
	 * public boolean downloadLogFile(String ip, String pemFilePath, String
	 * remoteFilePath, String localFilePath) {
	 * 
	 * JSch jsch = new JSch(); Session session = null; Channel channel = null;
	 * ChannelSftp sftpChannel = null;
	 * 
	 * try { // Add the PEM file as the private key for authentication
	 * jsch.addIdentity("C:\\Users\\BIS\\.ssh\\id_rsa");
	 * 
	 * // Create an SSH session session = jsch.getSession("ubuntu", ip, 22); //
	 * Replace 'ubuntu' with the server's username Properties config = new
	 * Properties(); config.put("StrictHostKeyChecking", "no");
	 * config.put("PreferredAuthentications", "publickey"); //
	 * session.setServerAliveInterval(60000); // 60 seconds //
	 * session.setServerAliveCountMax(5); session.setConfig(config);
	 * session.connect();
	 * 
	 * // Open an SFTP channel channel = session.openChannel("sftp");
	 * channel.connect(); sftpChannel = (ChannelSftp) channel;
	 * 
	 * // Download the file try (FileOutputStream fos = new
	 * FileOutputStream(localFilePath)) { InputStream inputStream =
	 * sftpChannel.get(remoteFilePath); byte[] buffer = new byte[1024]; int read;
	 * while ((read = inputStream.read(buffer)) != -1) { fos.write(buffer, 0, read);
	 * } fos.flush(); }
	 * 
	 * System.out.println("File downloaded successfully to: " + localFilePath);
	 * return true; } catch (Exception e) { e.printStackTrace(); return false; }
	 * finally { // Cleanup if (sftpChannel != null) { sftpChannel.disconnect(); }
	 * if (channel != null) { channel.disconnect(); } if (session != null) {
	 * session.disconnect(); } } }
	 */

	/*
	 * <<<----------------------- DYNAMIC EXCEL GENERATION OLD CODE
	 * ----------------------->>> private static final List<String> HEADERS =
	 * Arrays.asList("ID", "Name", "Email", "Salary", "Role", "Department");
	 * 
	 * // Sample data // Sample data using Map // Using mutable List of HashMaps
	 * private static final List<Map<String, String>> DATA = new ArrayList<>();
	 * 
	 * // static { // // Creating mutable employee records // DATA.add(new
	 * HashMap<>(Map.of("ID", "1", "Name", "John Doe", "Email", "john@example.com",
	 * "Salary", "5000", // "Role", "Developer"))); // // DATA.add(new
	 * HashMap<>(Map.of("ID", "2", "Name", "Jane Smith", "Email",
	 * "jane@example.com", "Salary", "6000", // "Role", "Manager"))); // //
	 * DATA.add(new HashMap<>(Map.of("ID", "3", "Name", "Sam Wilson", "Email",
	 * "sam@example.com", "Salary", "5500", // "Role", "Analyst"))); // } private
	 * List<Map<String, String>> getABCData() { return List.of( new
	 * LinkedHashMap<>(Map.of("ID", "1", "Name", "John Doe", "Email",
	 * "john@example.com", "Salary", "5000", "Role", "Developer")), new
	 * LinkedHashMap<>(Map.of("ID", "2", "Name", "Jane Smith", "Email",
	 * "jane@example.com", "Salary", "6000", "Role", "Manager")), new
	 * LinkedHashMap<>(Map.of("ID", "3", "Name", "Sam Wilson", "Email",
	 * "sam@example.com", "Salary", "5500", "Role", "Analyst"))); }
	 * 
	 * private List<Map<String, String>> getZYXData() { return List.of( new
	 * LinkedHashMap<>(Map.of("ID", "1", "Name", "John Doe", "Email",
	 * "john@example.com", "Salary", "5000", "Role", "Developer", "Department",
	 * "IT")), new LinkedHashMap<>(Map.of("ID", "2", "Name", "Jane Smith", "Email",
	 * "jane@example.com", "Salary", "6000", "Role", "Manager", "Department",
	 * "IT")), new LinkedHashMap<>(Map.of("ID", "3", "Name", "Sam Wilson", "Email",
	 * "sam@example.com", "Salary", "5500", "Role", "Analyst", "Department",
	 * "IT"))); }
	 * 
	 * 
	 * public void exportExcel(HttpServletResponse response) throws IOException {
	 * try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
	 * .registerWriteHandler(new CustomExcelStyle()) // ✅ Use the custom style
	 * handler .build()) {
	 * 
	 * Map<String, List<Map<String, String>>> companyData = new LinkedHashMap<>();
	 * companyData.put("ABC", getABCData()); companyData.put("ZYX", getZYXData());
	 * 
	 * for (Map.Entry<String, List<Map<String, String>>> entry :
	 * companyData.entrySet()) { String sheetName = entry.getKey(); List<Map<String,
	 * String>> companyEmployees = entry.getValue();
	 * 
	 * List<List<String>> dynamicHeaders = generateDynamicHeaders(companyEmployees);
	 * List<List<String>> convertedData = convertData(companyEmployees,
	 * dynamicHeaders);
	 * 
	 * WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).head(dynamicHeaders)
	 * .registerWriteHandler(new CustomExcelStyle()) // ✅ Apply custom style per
	 * sheet .build();
	 * 
	 * excelWriter.write(convertedData, writeSheet); } } }
	 * 
	 * // Generate dynamic headers // private List<List<String>> dynamicHeadings() {
	 * // return HEADERS.stream().map(header -> List.of(header)).toList(); // }
	 * private List<List<String>> generateDynamicHeaders(List<Map<String, String>>
	 * companyEmployees) { if (companyEmployees.isEmpty()) { return new
	 * ArrayList<>(); }
	 * 
	 * // Use LinkedHashSet to maintain insertion order and uniqueness
	 * LinkedHashSet<String> headerSet = new LinkedHashSet<>();
	 * 
	 * for (Map<String, String> employee : companyEmployees) {
	 * headerSet.addAll(employee.keySet()); // Maintain order dynamically }
	 * 
	 * // Convert LinkedHashSet to List (ordered headers) List<String> headers = new
	 * ArrayList<>(headerSet);
	 * 
	 * // Debugging: Print headers to verify order
	 * System.out.println("Generated Headers Order: " + headers);
	 * 
	 * return headers.stream().map(header -> List.of(header)).toList(); }
	 * 
	 * private List<List<String>> convertData(List<Map<String, String>> employees,
	 * List<List<String>> dynamicHeaders) { // Extract header names in correct order
	 * List<String> headers = dynamicHeaders.stream().map(h -> h.get(0)).toList();
	 * 
	 * // Debugging: Print headers before processing data
	 * System.out.println("Final Headers Used: " + headers);
	 * 
	 * return employees.stream().map(row -> { // Debugging: Print row before
	 * transformation System.out.println("Processing row: " + row);
	 * 
	 * return headers.stream().map(header -> row.getOrDefault(header, "")) // Ensure
	 * consistent column order .toList(); }).toList(); }
	 * 
	 * // // Custom Styling for Header & Data public static class CustomExcelStyle
	 * extends AbstractVerticalCellStyleStrategy implements WriteHandler {
	 * 
	 * @Override protected WriteCellStyle headCellStyle(CellWriteHandlerContext
	 * context) { WriteCellStyle writeCellStyle = new WriteCellStyle();
	 * writeCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
	 * writeCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
	 * writeCellStyle.setWrapped(true);
	 * writeCellStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
	 * 
	 * // Use EasyExcel WriteFont instead of POI Font WriteFont writeFont = new
	 * WriteFont(); writeFont.setBold(true);
	 * writeFont.setColor(IndexedColors.WHITE.getIndex());
	 * writeCellStyle.setWriteFont(writeFont);
	 * 
	 * return writeCellStyle; }
	 * 
	 * @Override protected WriteCellStyle contentCellStyle(CellWriteHandlerContext
	 * context) { WriteCellStyle writeCellStyle = new WriteCellStyle();
	 * writeCellStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);
	 * writeCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
	 * writeCellStyle.setWrapped(true);
	 * 
	 * // Use EasyExcel WriteFont instead of POI Font WriteFont writeFont = new
	 * WriteFont(); writeFont.setBold(false);
	 * writeFont.setColor(IndexedColors.BLACK.getIndex());
	 * writeCellStyle.setWriteFont(writeFont);
	 * 
	 * return writeCellStyle; } }
	 */
}
