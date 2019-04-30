package com.etermax.jsonb.orm.builder

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class HikariDataSourceBuilder {

	private lateinit var url: String
	private lateinit var user: String
	private lateinit var password: String
	private var poolName = "defaultpoolname"
	private var maximumPoolSize = 5
	private var connectionTimeout = 1000L
	private var connectionLifetime = 1800000L

	companion object {
		@JvmStatic
		fun default(): HikariDataSourceBuilder = HikariDataSourceBuilder()
	}

	fun build(): HikariDataSource {
		val hikariConfig = HikariConfig()
		hikariConfig.jdbcUrl = url
		hikariConfig.username = user
		hikariConfig.password = password
		hikariConfig.maximumPoolSize = maximumPoolSize
		hikariConfig.connectionTimeout = connectionTimeout
		hikariConfig.maxLifetime = connectionLifetime
		hikariConfig.poolName = poolName
		return HikariDataSource(hikariConfig)
	}

	fun withUrl(url: String): HikariDataSourceBuilder {
		this.url = url;
		return this;
	}

	fun withUser(user: String): HikariDataSourceBuilder {
		this.user = user;
		return this;
	}

	fun withPassword(password: String): HikariDataSourceBuilder {
		this.password = password;
		return this;
	}

	fun withPoolName(poolName: String): HikariDataSourceBuilder {
		this.poolName = poolName;
		return this;
	}

	fun withMaximumPoolSize(maximumPoolSize: Int): HikariDataSourceBuilder {
		this.maximumPoolSize = maximumPoolSize;
		return this;
	}

	fun withConnectionTimeout(connectionTimeout: Long): HikariDataSourceBuilder {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	fun withConnectionLifetime(connectionLifetime: Long): HikariDataSourceBuilder {
		this.connectionLifetime = connectionLifetime;
		return this;
	}
}