package com.etermax.jsonb.orm.mocks;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.sql.ResultSet;
import java.util.function.Consumer;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.etermax.jsonb.orm.PostgresConnector;
import com.etermax.jsonb.orm.exceptions.PostgresConnectionException;

public class PostgresConnectorMock {
	private String nextValQuery;

	@Mock
	private PostgresConnector connector;

	public PostgresConnectorMock() {
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

	public PostgresConnector getMock() {
		return connector;
	}

	public void verfyExecute(String query) {
		Mockito.verify(connector, times(1)).execute(query);
	}
}
