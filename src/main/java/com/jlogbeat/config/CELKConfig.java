package com.jlogbeat.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class CELKConfig {

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any()).build();
	}

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
