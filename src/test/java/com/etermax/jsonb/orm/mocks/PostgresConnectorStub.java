package com.etermax.jsonb.orm.mocks;

import static java.util.stream.IntStream.rangeClosed;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import com.etermax.jsonb.orm.PostgresConnector;
import com.etermax.jsonb.orm.exceptions.PostgresConnectionException;

public class PostgresConnectorStub {
	private String nextValQuery;

	@Mock
	private PostgresConnector connector;

	public PostgresConnectorStub() {
		MockitoAnnotations.initMocks(this);
	}

	public void whenExecuteNextValWithQueryReturns(String expectedQuery, Long val) {
		doAnswer(it -> {
			nextValQuery = (String) it.getArguments()[0];
			if (nextValQuery.equals(expectedQuery)) {
				ResultSet resultSet = mock(ResultSet.class);
				Mockito.when(resultSet.getLong(1)).thenReturn(val);
				((Consumer) it.getArguments()[1]).accept(resultSet);
			} else {
				throw new PostgresConnectionException();
			}
			return null;
		}).when(connector).executeNextVal(Mockito.anyString(), Mockito.any(Consumer.class));

	}

	public void whenExecuteOnWritingNodeQueryThenReturnResult(String expectedQuery, String... values) {
		Stubber stubbedAnswer = getStubbedAnswer(expectedQuery, values);
		stubbedAnswer.when(connector).executeOnWriteNode(Mockito.anyString(), Mockito.any(Consumer.class));
	}

	public void whenExecuteQueryThenReturnResult(String expectedQuery, String... values) {
		Stubber stubbedAnswer = getStubbedAnswer(expectedQuery, values);
		stubbedAnswer.when(connector).execute(Mockito.anyString(), Mockito.any(Consumer.class));
	}

	private Stubber getStubbedAnswer(String expectedQuery, String[] values) {
		return doAnswer(it -> {
			nextValQuery = (String) it.getArguments()[0];
			if (nextValQuery.equals(expectedQuery)) {
				ResultSet resultSet = mock(ResultSet.class);

				stubNextMethod(resultSet, values);

				stubGetStringMethod(resultSet, values);

				((Consumer) it.getArguments()[1]).accept(resultSet);
			} else {
				throw new PostgresConnectionException();
			}
			return null;
		});
	}

	private void stubGetStringMethod(ResultSet resultSet, String[] values) throws SQLException {
		Stubber jsonStubber = doReturn(values[0]);
		rangeClosed(1, values.length - 1).asLongStream().forEach((i) -> {
			jsonStubber.doReturn(values[(int) i]);
		});
		jsonStubber.when(resultSet).getString("entity");
	}

	private void stubNextMethod(ResultSet resultSet, String[] values) throws SQLException {
		Stubber nextStubber = doReturn(true);
		rangeClosed(0, values.length - 1).asLongStream().forEach((i) -> {
			nextStubber.doReturn(i != values.length - 1);
		});
		nextStubber.when(resultSet).next();
	}

	public PostgresConnector getMock() {
		return connector;
	}

	public void verfyExecute(String query) {
		Mockito.verify(connector, times(1)).execute(query);
	}
}
