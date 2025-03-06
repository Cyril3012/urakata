package com.urakata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerPort {
	private String serviceName;

	private int port;
	private String status;
	// Getters and Setters
}
