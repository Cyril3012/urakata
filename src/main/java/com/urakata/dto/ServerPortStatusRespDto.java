package com.urakata.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerPortStatusRespDto {
	private String serverName;
	private String projectTag;
	private String serverIp;
	private List<ServerPort> serverPortList;

	// Getters and Setters
}
