package com.urakata.model;

import java.time.LocalDateTime;

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
@Table(name = "system_metrics_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "server_name", nullable = false)
	private String serverName;

	@Column(name = "metric_type", nullable = false)
	private String metricType; // e.g., cpu, memory, disk

	@Column(name = "usage_percentage", nullable = false)
	private Double usagePercentage; // 0 to 100

	@Column(name = "timestamp", nullable = false)
	private LocalDateTime timestamp;
}
