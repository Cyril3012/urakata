package com.urakata.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "server_master") // H2 doesn't support hyphen in table names
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServerDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "SERVERNAME")
	private String serverName;

	@Column(name = "SERVERIP")
	private String serverIp;

	@Column(name = "SERVICES")
	private String services;

	@Column(name = "FILESYSTEM")
	private String fileSystem;

	@Column(name = "USERNAME")
	private String username;

	@Column(name = "PASSWORD")
	private String password;

	@Column(name = "PEMFILEPATH")
	private String pemFilePath;

	@Column(name = "LOCALPUBLICKEYPATH")
	private String localPublicKeyPath;

	@Column(name = "SERVERMAINTENANCEFREQUENCE")
	private String serverMaintenanceFrequence;

	@Column(name = "OSTYPE")
	private String osType;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// Custom Getter for services (JSON to List<ServiceDto>)
	public List<ServiceDto> getServices() {
		try {
			return services != null
					? objectMapper.readValue(services,
							objectMapper.getTypeFactory().constructCollectionType(List.class, ServiceDto.class))
					: Collections.emptyList(); // Return an empty list instead of null
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error parsing services JSON", e);
		}
	}

	// Custom Setter for services (List<ServiceDto> to JSON)
	public void setServices(List<ServiceDto> servicesList) {
		try {
			this.services = objectMapper.writeValueAsString(servicesList);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting services to JSON", e);
		}
	}

	// Custom Getter for fileSystem (JSON to List<FileSystemDto>)
	public List<FileSystemDto> getFileSystem() {
		try {
			return fileSystem != null
					? objectMapper.readValue(fileSystem,
							objectMapper.getTypeFactory().constructCollectionType(List.class, FileSystemDto.class))
					: null;
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error parsing fileSystem JSON", e);
		}
	}

	// Custom Setter for fileSystem (List<FileSystemDto> to JSON)
	public void setFileSystem(List<FileSystemDto> fileSystemList) {
		try {
			this.fileSystem = objectMapper.writeValueAsString(fileSystemList);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error converting fileSystem to JSON", e);
		}
	}

	// Inner DTO Class for Service
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ServiceDto {
		private String name;
		private int port;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}
	}

	// Inner DTO Class for FileSystem
	@AllArgsConstructor
	@NoArgsConstructor
	public static class FileSystemDto {
		private String name;
		private String logpath;
		private String backupPath;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLogpath() {
			return logpath;
		}

		public void setLogpath(String logpath) {
			this.logpath = logpath;
		}

		public String getBackupPath() {
			return backupPath;
		}

		public void setBackupPath(String backupPath) {
			this.backupPath = backupPath;
		}
	}
}
