package com.etermax.jsonb.orm

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.times
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.function.Consumer

class PostgresConnectorTest() {

	private lateinit var connector: PostgresConnector
	@Mock
	private lateinit var readDataSource: HikariDataSource
	@Mock
	private lateinit var readConnection: Connection
	@Mock
	private lateinit var readStatement: Statement
	@Mock
	private lateinit var writeDataSource: HikariDataSource
	@Mock
	private lateinit var writeConnection: Connection
	@Mock
	private lateinit var writeStatement: PreparedStatement
	@Mock
	private lateinit var resultSet: ResultSet

	private lateinit var queryTimeout: Integer

	@Before
	fun setUp() {
		MockitoAnnotations.initMocks(this)
		Mockito.`when`(readDataSource.getConnection()).thenReturn(readConnection)
		Mockito.`when`(readConnection.createStatement()).thenReturn(readStatement)
		Mockito.`when`(writeDataSource.getConnection()).thenReturn(writeConnection)
		Mockito.`when`(writeConnection.prepareStatement(Mockito.anyString())).thenReturn(writeStatement)
		connector = PostgresConnector(readDataSource, writeDataSource, 1, "");
	}


	@Test
	fun whenExecuteRead_queryTimeoutIsCalled() {
		doAnswer { queryTimeout = it.arguments[0] as Integer }.`when`(readStatement).setQueryTimeout(Mockito.anyInt())

		connector.execute("mock", Consumer { })

		assertThat(queryTimeout).isEqualTo(1);
	}


	@Test
	fun whenExecuteReadCommand_correctEcecution_dataSourcedIsPassedToConsumer() {
		Mockito.`when`(readStatement.executeQuery(Mockito.anyString())).thenReturn(resultSet);

		connector.execute("some select sql", Consumer {
			assertThat(it).isSameAs(resultSet)
		})
	}

	@Test
	fun whenExecuteWrite_queryTimeoutIsCalled() {
		doAnswer { queryTimeout = it.arguments[0] as Integer }.`when`(writeStatement).setQueryTimeout(Mockito.anyInt())

		connector.execute("mock")

		assertThat(queryTimeout).isEqualTo(1);
	}

	@Test
	fun whenExecuteWriteCommand_withErrorInExecution_ExceptionIsExpected() {
		connector.execute("some insert or update")

		Mockito.verify(writeStatement, times(1)).executeUpdate()
	}
}