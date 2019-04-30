package com.etermax.jsonb.orm;

import static com.etermax.jsonb.orm.exceptions.ExceptionCatcher.executeOrRuntime;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.etermax.jsonb.orm.domain.SomeJsonbEntity;
import com.etermax.jsonb.orm.mocks.PostgresConnectorMock;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonbEntityManagerTest {

	private JsonbEntityManager entityManager;
	private PostgresConnectorMock connectorMock;
	private static long nextSequenceVal = 1L;
	private ObjectMapper objectMapper;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TableNamesResolver tableNamesResolver = new TableNamesResolver("com.etermax.jsonb.orm");
		connectorMock = new PostgresConnectorMock();
		objectMapper = new ObjectMapper();
		entityManager = new JsonbEntityManager(objectMapper, connectorMock.getMock(), tableNamesResolver);
	}

	@Test
	public void saveNewEntity_thenEntityHasIdAndInsertQueryIsExecuted() {
		SomeJsonbEntity entity = new SomeJsonbEntity("anIndexedValue");
		long id = nextSequenceVal++;
		connectorMock.whenExecuteNextValWithQueryReturns("select nextval('seq_test_table')", id);

		entityManager.save(entity);

		assertThat(entity.getId()).isNotNull();
		assertThat(entity.getId()).isEqualTo(id);
		String jsonEntity = executeOrRuntime(() -> objectMapper.writeValueAsString(entity));
		connectorMock.verfyExecute("INSERT INTO test_table (id, entity) VALUES (" + id + ", '" + jsonEntity + "');");
	}

	@Test
	public void saveExistentEntity_thenUpdatetQueryIsExecuted() {
		SomeJsonbEntity entity = new SomeJsonbEntity("anIndexedValue");
		entity.setId(nextSequenceVal++);
		connectorMock.whenExecuteNextValWithQueryReturns("select nextval('seq_test_table')", null);

		entityManager.save(entity);

		assertThat(entity.getId()).isNotNull();
		String jsonEntity = executeOrRuntime(() -> objectMapper.writeValueAsString(entity));
		connectorMock.verfyExecute("update test_table set entity= '" + jsonEntity + "' where id = " + entity.getId() + ";");
	}
}