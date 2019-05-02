package com.etermax.jsonb.mocks;

import static com.etermax.jsonb.exceptions.ExceptionCatcher.executeOrRuntime;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import com.etermax.jsonb.PostgresConnector;
import com.etermax.jsonb.exceptions.PostgresConnectionException;

public class PostgresConnectorStub {
	private Map<String, ResultSet> resultSetByQuery = newHashMap();

	private List<String> executedQueries = newArrayList();

	@Mock
	private PostgresConnector connector;

	public PostgresConnectorStub() {
		MockitoAnnotations.initMocks(this);
		Mockito.doAnswer(it -> executedQueries.add((String) it.getArguments()[0])).when(connector).execute(Mockito.anyString());
		Mockito.doCallRealMethod().when(connector).executeExist(Mockito.anyString(), Mockito.any(Consumer.class));
	}

	public void whenExecuteNextValWithQueryReturns(String expectedQuery, Long val) {
		doAnswer(it -> {
			String nextValQuery = (String) it.getArguments()[0];
			executedQueries.add(nextValQuery);
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

	public void whenExecuteOnWritingNodeQueryThenReturnResult(String expectedQuery, Object... values) {
		Stubber stubbedAnswer = getStubbedAnswer(expectedQuery, values);
		stubbedAnswer.when(connector).executeOnWriteNode(Mockito.anyString(), Mockito.any(Consumer.class));
	}

	public void whenExecuteQueryThenReturnResult(String expectedQuery, Object... values) {
		Stubber stubbedAnswer = getStubbedAnswer(expectedQuery, values);
		stubbedAnswer.when(connector).execute(Mockito.anyString(), Mockito.any(Consumer.class));
	}

	private Stubber getStubbedAnswer(String expectedQuery, Object[] values) {
		resultSetByQuery.put(expectedQuery, executeOrRuntime(() -> getResultSet(values)));
		return doAnswer(it -> {
			String currentQuery = (String) it.getArguments()[0];
			executedQueries.add(currentQuery);
			if (resultSetByQuery.keySet().contains(currentQuery)) {
				((Consumer) it.getArguments()[1]).accept(resultSetByQuery.get(currentQuery));
			} else {
				throw new PostgresConnectionException();
			}
			return null;
		});
	}

	private ResultSet getResultSet(Object[] values) throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);

		stubNextMethod(resultSet, values);

		if (values.length > 0 && values[0] instanceof String) {
			stubGetStringMethod(resultSet, values);
		}
		if (values.length > 0 && values[0] instanceof Boolean) {
			stubGetBooleanMethod(resultSet, values);
		}
		stubGetObjectMethod(resultSet, values);
		return resultSet;
	}

	private void stubGetStringMethod(ResultSet resultSet, Object[] values) throws SQLException {
		Stubber jsonStubber = values.length == 0 ? doReturn(null) : doReturn(values[0]);
		rangeClosed(1, values.length - 1).asLongStream().forEach((i) -> {
			jsonStubber.doReturn(values[(int) i]);
		});
		jsonStubber.when(resultSet).getString("entity");
	}

	private void stubGetObjectMethod(ResultSet resultSet, Object[] values) throws SQLException {
		Stubber jsonStubber = values.length == 0 ? doReturn(null) : doReturn(values[0]);
		rangeClosed(1, values.length - 1).asLongStream().forEach((i) -> {
			jsonStubber.doReturn(values[(int) i]);
		});
		jsonStubber.when(resultSet).getObject(1);
	}

	private void stubGetBooleanMethod(ResultSet resultSet, Object[] values) throws SQLException {
		Stubber jsonStubber = values.length == 0 ? doReturn(null) : doReturn(values[0]);
		rangeClosed(1, values.length - 1).asLongStream().forEach((i) -> {
			jsonStubber.doReturn(values[(int) i]);
		});
		jsonStubber.when(resultSet).getBoolean(1);
	}

	private void stubNextMethod(ResultSet resultSet, Object[] values) throws SQLException {
		Stubber nextStubber = doReturn(values.length > 0);
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

	public void assertThatQueryWasExecuted(String query) {
		assertThat(executedQueries).contains(query);
	}

	public void assertThatQueryWasNotExecuted(String query) {
		assertThat(executedQueries).doesNotContain(query);
	}

}
