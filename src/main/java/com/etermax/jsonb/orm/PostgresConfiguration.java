package com.etermax.jsonb.orm;

import org.apache.commons.lang3.StringUtils;

public class PostgresConfiguration {

	private String url;
	private String user;
	private String password;
	private int poolSize;

	public PostgresConfiguration(String url, String user, String password, int poolSize) {
		this.url = url;
		this.user = user;
		this.password = password;
		this.poolSize = poolSize;
	}

	public String getUrl() {
		String postgresUrl = System.getenv("POSTGRES_URL");
		if (StringUtils.isNotBlank(postgresUrl)) {
			return postgresUrl;
		}
		return url;
	}

	public String getPassword() {
		return password;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public String getUser() {
		return user;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}
}
