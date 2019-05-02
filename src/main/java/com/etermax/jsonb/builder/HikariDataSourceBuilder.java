package com.etermax.jsonb.builder;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariDataSourceBuilder {

	private String url;
	private String user;
	private String password;
	private String poolName = "defaultpoolname";
	private Integer maximumPoolSize = 5;
	private Long connectionTimeout = 1000L;
	private Long connectionLifetime = 1800000L;

	public static HikariDataSourceBuilder defaultDataSource() {
		return new HikariDataSourceBuilder();
	}

	public HikariDataSource build() {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(url);
		hikariConfig.setUsername(user);
		hikariConfig.setPassword(password);
		hikariConfig.setMaximumPoolSize(maximumPoolSize);
		hikariConfig.setConnectionTimeout(connectionTimeout);
		hikariConfig.setMaxLifetime(connectionLifetime);
		hikariConfig.setPoolName(poolName);
		return new HikariDataSource(hikariConfig);
	}

	public HikariDataSourceBuilder withUrl(String url) {
		this.url = url;
		return this;
	}

	public HikariDataSourceBuilder withUser(String user) {
		this.user = user;
		return this;
	}

	public HikariDataSourceBuilder withPassword(String password) {
		this.password = password;
		return this;
	}

	public HikariDataSourceBuilder withPoolName(String poolName) {
		this.poolName = poolName;
		return this;
	}

	public HikariDataSourceBuilder withMaximumPoolSize(Integer maximumPoolSize) {
		this.maximumPoolSize = maximumPoolSize;
		return this;
	}

	public HikariDataSourceBuilder withConnectionTimeout(Long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	public HikariDataSourceBuilder withConnectionLifetime(Long connectionLifetime) {
		this.connectionLifetime = connectionLifetime;
		return this;
	}
}