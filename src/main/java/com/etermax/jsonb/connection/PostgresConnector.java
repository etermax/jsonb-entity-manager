package com.etermax.jsonb.connection;

import static com.etermax.jsonb.exceptions.ExceptionCatcher.executeOrRuntime;
import static java.lang.String.format;

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
	private static final Logger logger = LoggerFactory.getLogger(PostgresConnector.class);
	public static final String ERROR = "ERROR PG Executing: ";
	private static final String LOG_MESSAGE = "%s time: %d %s PG Executing: %s";
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
			logError(query, e);
			throw new PostgresConnectionException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logInfo((finalTime - initialTime), "read", "read datasource", query);

	}

	public void execute(String query) {
		long initialTime = ZonedDateTime.now().toInstant().toEpochMilli();
		try (Connection conn = writeDataSource.getConnection(); PreparedStatement statement = conn.prepareStatement(query)) {
			statement.setQueryTimeout(queryTimeout);
			statement.executeUpdate();
		} catch (Exception e) {
			logError(query, e);
			throw new PostgresConnectionException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logInfo((finalTime - initialTime), "write", "write datasource", query);
	}

	public void executeOnWriteNode(String query, Consumer<ResultSet> consumer) {
		long initialTime = DateTime.now().getMillis();
		try (Connection conn = writeDataSource.getConnection(); Statement st = conn.createStatement()) {
			st.setQueryTimeout(queryTimeout);
			try (ResultSet rs = st.executeQuery(query)) {
				consumer.accept(rs);
			}
		} catch (Exception e) {
			logError(query, e);
			throw new PostgresConnectionException(e);
		}
		long finalTime = DateTime.now().getMillis();
		logInfo((finalTime - initialTime), "read", "write datasource", query);

	}

	private void logInfo(long time, String opperationType, String overDataSource, String query) {
		String info = format(LOG_MESSAGE, opperationType, time, overDataSource, query);
		logger.info(info);
	}

	private void logError(String query, Exception e) {
		String error = ERROR + query;
		logger.error(error, e);
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
