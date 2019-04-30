package com.etermax.jsonb.orm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.etermax.jsonb.orm.exceptions.PostgresConnectionException;
import com.zaxxer.hikari.HikariDataSource;

public class PostgresConnectorTest {

	private PostgresConnector connector;
	@Mock
	private HikariDataSource readDataSource;
	@Mock
	private Connection readConnection;
	@Mock
	private Statement readStatement;
	@Mock
	private HikariDataSource writeDataSource;
	@Mock
	private Connection writeConnection;
	@Mock
	private PreparedStatement writeStatement;
	@Mock
	private ResultSet resultSet;

	private Integer queryTimeout;

	@Before
	public void setUp() throws SQLException {
		MockitoAnnotations.initMocks(this);
		when(readDataSource.getConnection()).thenReturn(readConnection);
		when(readConnection.createStatement()).thenReturn(readStatement);
		when(writeDataSource.getConnection()).thenReturn(writeConnection);
		when(writeConnection.prepareStatement(Mockito.anyString())).thenReturn(writeStatement);
		when(writeConnection.createStatement()).thenReturn(writeStatement);
		connector = new PostgresConnector(readDataSource, writeDataSource);
	}

	@Test
	public void whenExecuteRead_queryTimeoutIsCalled() throws SQLException {
		doAnswer(it -> {
			queryTimeout = (Integer) it.getArguments()[0];
			return null;
		}).when(readStatement).setQueryTimeout(Mockito.anyInt());

		connector.execute("mock", (a) -> {
		});

		assertThat(queryTimeout).isEqualTo(1);
	}

	@Test
	public void whenExecuteRead_correctExecution_dataSourcedIsPassedToConsumer() throws SQLException {
		when(readStatement.executeQuery(Mockito.anyString())).thenReturn(resultSet);

		connector.execute("some select sql", (it) -> assertThat(it).isSameAs(resultSet));
	}

	@Test
	public void whenExecuteExisits_correctExecution_dataSourcedIsPassedToConsumer() throws SQLException {
		when(readStatement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
		when(resultSet.getBoolean(1)).thenReturn(true);

		connector.executeExist("some exist sql", (it) -> assertThat(it).isTrue());
	}

	@Test
	public void whenExecuteCount_correctExecution_dataSourcedIsPassedToConsumer() throws SQLException {
		when(readStatement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
		when(resultSet.getLong(1)).thenReturn(2L);

		connector.executeCount("some count sql", (it) -> assertThat(it).isEqualTo(2L));
	}

	@Test
	public void whenExecuteRead_errorOnExecution_exceptionThrown() throws SQLException {
		when(readStatement.executeQuery(Mockito.anyString())).thenThrow(RuntimeException.class);

		assertThatThrownBy(() -> {
			connector.execute("some select sql", (a) -> {
			});
		}).isInstanceOf(PostgresConnectionException.class);
	}

	@Test
	public void whenExecuteWrite_queryTimeoutIsCalled() throws SQLException {
		doAnswer(it -> {
			queryTimeout = (Integer) it.getArguments()[0];
			return null;
		}).when(writeStatement).setQueryTimeout(Mockito.anyInt());

		connector.execute("mock");

		assertThat(queryTimeout).isEqualTo(1);
	}

	@Test
	public void whenExecuteWrite_withErrorInExecution_ExceptionIsExpected() throws SQLException {
		connector.execute("some insert or update");

		Mockito.verify(writeStatement, Mockito.times(1)).executeUpdate();
	}

	@Test
	public void whenExecuteWrite_errorOnExecution_exceptionThrown() throws SQLException {
		when(writeStatement.executeUpdate()).thenThrow(RuntimeException.class);

		assertThatThrownBy(() -> connector.execute("some select sql")).isInstanceOf(PostgresConnectionException.class);
	}

	@Test
	public void executeNextVal_queryTimeoutIsCalled() throws SQLException {
		doAnswer(it -> {
			queryTimeout = (Integer) it.getArguments()[0];
			return null;
		}).when(writeStatement).setQueryTimeout(Mockito.anyInt());

		connector.executeNextVal("mock", (a) -> {
		});

		assertThat(queryTimeout).isEqualTo(1);
	}

	@Test
	public void executeNextVal_correctExecution_dataSourcedIsPassedToConsumer() throws SQLException {
		when(writeStatement.executeQuery(Mockito.anyString())).thenReturn(resultSet);

		connector.executeNextVal("some next val sql", (it) -> assertThat(it).isSameAs(resultSet));
	}

	@Test
	public void executeNextVal_errorOnExecution_exceptionThrown() throws SQLException {
		when(writeStatement.executeQuery(Mockito.anyString())).thenThrow(RuntimeException.class);

		assertThatThrownBy(() -> connector.executeNextVal("some select sql", (it) -> {
		})).isInstanceOf(PostgresConnectionException.class);
	}

}