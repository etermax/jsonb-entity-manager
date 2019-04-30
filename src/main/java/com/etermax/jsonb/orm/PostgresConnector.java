package com.etermax.jsonb.orm;

import static com.etermax.jsonb.orm.exceptions.ExceptionCatcher.executeOrRuntime;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class PostgresConnector {
	private static final String EXECUTING = "PG Executing: ";
	private static final Logger logger = LoggerFactory.getLogger(PostgresConnector.class);
	private ComboPooledDataSource cpdsRead;
	private ComboPooledDataSource cpdsWrite;

	public PostgresConnector(PostgresConfiguration configRead, PostgresConfiguration configWrite) {
		cpdsRead = configureDatasource(configRead);
		cpdsWrite = configureDatasource(configWrite);
	}

	public PostgresConnector(PostgresConfiguration config) {
		cpdsRead = configureDatasource(config);
		cpdsWrite = cpdsRead;
	}

	private ComboPooledDataSource configureDatasource(PostgresConfiguration config) {
		ComboPooledDataSource cpds = new ComboPooledDataSource();
		cpds.setJdbcUrl(config.getUrl());
		cpds.setUser(config.getUser());
		cpds.setPassword(config.getPassword());
		cpds.setInitialPoolSize(config.getPoolSize() / 2);
		cpds.setMinPoolSize(config.getPoolSize() / 2);
		cpds.setAcquireIncrement(5);
		cpds.setMaxPoolSize(config.getPoolSize());
		cpds.setMaxStatements(100);
		return cpds;
	}

	public void execute(String query, Consumer<ResultSet> consumer) {
		long initialTime = ZonedDateTime.now().toInstant().toEpochMilli();
		try (Connection conn = cpdsRead.getConnection(); Statement st = conn.createStatement()) {
			st.setQueryTimeout(1);
			try (ResultSet rs = st.executeQuery(query)) {
				consumer.accept(rs);
			}
		} catch (Exception e) {
			logger.error("ERROR " + EXECUTING + query, e);
			throw new RuntimeException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logger.info(" read time: " + (finalTime - initialTime) + EXECUTING + query);

	}

	public void executeByteArrayInsert(long id, String name, byte[] bytes) {
		long initialTime = ZonedDateTime.now().toInstant().toEpochMilli();
		try (Connection conn = cpdsWrite.getConnection();
				PreparedStatement ps = conn.prepareStatement("INSERT INTO bytes_files (id , name , b) VALUES(?,?,?)")) {
			ps.setLong(1, id);
			ps.setString(2, name);
			ps.setBinaryStream(3, new ByteArrayInputStream(bytes), bytes.length);
			ps.setQueryTimeout(5);
			ps.executeUpdate();
		} catch (Exception e) {
			logger.error("ERROR " + EXECUTING + "INSERT INTO bytes_files (id , name , b) VALUES(" + id + "," + name + ",?)", e);
			throw new RuntimeException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logger.info(" write time: " + (finalTime - initialTime) + EXECUTING + "INSERT INTO bytes_files (id , name , b ) VALUES(" + id + "," + name
				+ ",?)");
	}

	public byte[] executeFindByteArray(long id) {
		long initialTime = ZonedDateTime.now().toInstant().toEpochMilli();
		byte[] imgBytes = null;
		try (Connection conn = cpdsWrite.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT b FROM bytes_files WHERE  id = ?")) {
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					imgBytes = rs.getBytes(1);
				}
			}
		} catch (Exception e) {
			logger.error("ERROR " + EXECUTING + "SELECT img FROM images WHERE  id = ?", e);
			throw new RuntimeException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logger.info(" write time: " + (finalTime - initialTime) + EXECUTING + "SELECT img FROM images WHERE  id = ?");
		return imgBytes;
	}

	public void execute(String query) {
		long initialTime = ZonedDateTime.now().toInstant().toEpochMilli();
		try (Connection conn = cpdsWrite.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setQueryTimeout(5);
			ps.executeUpdate();
		} catch (Exception e) {
			logger.error("ERROR " + EXECUTING + query, e);
			throw new RuntimeException(e);
		}
		long finalTime = ZonedDateTime.now().toInstant().toEpochMilli();
		logger.info(" write time: " + (finalTime - initialTime) + EXECUTING + query);
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
