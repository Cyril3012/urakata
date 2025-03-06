package com.urakata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Servers {
	private String serverName;
	private String ip;
	private String pemFilePath;
}
