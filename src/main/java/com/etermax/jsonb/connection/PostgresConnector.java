package com.etermax.jsonb.connection;

import static com.etermax.jsonb.exceptions.ExceptionCatcher.executeOrRuntime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.etermax.jsonb.exceptions.PostgresConnectionException;
import com.zaxxer.hikari.HikariDataSource;

public class PostgresConnector {
	private static final String EXECUTING = "PG Executing: ";
	private static final Logger logger = LoggerFactory.getLogger(PostgresConnector.class);
	private HikariDataSource readDataSource;
	private HikariDataSource writeDataSource;
	private int queryTimeout;
	private String url;

	public PostgresConnector(HikariDataSource readDataSource, HikariDataSource writeDataSource) {
		this(readDataSource, writeDataSource, 1, "");
	}

	public PostgresConnector(HikariDataSource readDataSource, HikariDataSource writeDataSource, int queryTimeout, String url) {
		this.readDataSource = readDataSource;
		this.writeDataSource = writeDataSource;
		this.queryTimeout = queryTimeout;
		this.url = url;
	}

	public void execute(String query, Consumer<ResultSet> consumer) {
		long initialTime = ZonedDateTime.now().toInstant().toEpochMilli();
		try (Connection connection = readDataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.setQueryTimeout(queryTimeout);
			try (ResultSet rs = statement.executeQuery(query)) {
				consumer.accept(rs);
			}
		} catch (Exception e) {
			logger.error("ERROR " + EXECUTING + query, e);
			throw new PostgresConnectionException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logger.info(" read time: " + (finalTime - initialTime) + EXECUTING + query);

	}

	public void execute(String query) {
		long initialTime = ZonedDateTime.now().toInstant().toEpochMilli();
		try (Connection conn = writeDataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(query)) {
			statement.setQueryTimeout(queryTimeout);
			statement.executeUpdate();
		} catch (Exception e) {
			logger.error("ERROR " + EXECUTING + query, e);
			throw new PostgresConnectionException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logger.info(" write time: " + (finalTime - initialTime) + EXECUTING + query);
	}

	public void executeOnWriteNode(String query, Consumer<ResultSet> consumer) {
		long initialTime = DateTime.now().getMillis();
		try (Connection conn = writeDataSource.getConnection(); Statement st = conn.createStatement()) {
			st.setQueryTimeout(queryTimeout);
			try (ResultSet rs = st.executeQuery(query)) {
				consumer.accept(rs);
			}
		} catch (Exception e) {
			logger.error("ERROR " + url + " " + EXECUTING + query, e);
			throw new PostgresConnectionException(e);
		}
		long finalTime = DateTime.now().getMillis();
		logger.info(" read time: " + (finalTime - initialTime) + " " + url + " " + EXECUTING + query);

	}

	public void executeNextVal(String query, Consumer<ResultSet> consumer) {
		executeOnWriteNode(query, consumer);
	}

	public void executeExist(String query, Consumer<Boolean> consumer) {
		execute(query, rs -> executeOrRuntime(() -> {
			rs.next();
			consumer.accept(rs.getBoolean(1));
		}));
	}

	public void executeCount(String query, Consumer<Long> consumer) {
		execute(query, rs -> executeOrRuntime(() -> {
			rs.next();
			consumer.accept(rs.getLong(1));
		}));
	}
}
