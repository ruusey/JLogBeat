package com.cybrary.celk.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CELKConfig {
	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(20);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("WINLOG-");
		return executor;
	}

	@Bean
	public ExecutorService executorService() {
		return Executors.newFixedThreadPool(20, this.taskExecutor());
	}

	@Bean
	public DataSource getDataSource() throws IllegalStateException {

		final DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
		dataSourceBuilder.driverClassName("com.mysql.cj.jdbc.Driver");
		dataSourceBuilder.url("jdbc:mysql://" + "192.168.1.72" + ":3306/jwinlog");
		dataSourceBuilder.username("root");
		dataSourceBuilder.password("Database123");
		return dataSourceBuilder.build();
	}
}
