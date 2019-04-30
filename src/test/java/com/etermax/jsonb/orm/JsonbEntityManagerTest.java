package com.etermax.jsonb.orm;

import static com.etermax.jsonb.orm.exceptions.ExceptionCatcher.executeOrRuntime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.etermax.jsonb.orm.domain.SomeJsonbEntity;
import com.etermax.jsonb.orm.mocks.PostgresConnectorStub;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonbEntityManagerTest {

	private JsonbEntityManager entityManager;
	private PostgresConnectorStub connectorMock;
	private static long nextSequenceVal = 1L;
	private ObjectMapper objectMapper;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TableNamesResolver tableNamesResolver = new TableNamesResolver("com.etermax.jsonb.orm");
		connectorMock = new PostgresConnectorStub();
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
		connectorMock.verfyExecute("INSERT INTO test_table (id, entity) VALUES (" + id + ", '" + serialize(entity) + "');");
	}

	@Test
	public void saveExistentEntity_thenUpdatetQueryIsExecuted() {
		SomeJsonbEntity entity = new SomeJsonbEntity("anIndexedValue");
		entity.setId(nextSequenceVal++);
		connectorMock.whenExecuteNextValWithQueryReturns("select nextval('seq_test_table')", null);

		entityManager.save(entity);

		assertThat(entity.getId()).isNotNull();
		connectorMock.verfyExecute("update test_table set entity= '" + serialize(entity) + "' where id = " + entity.getId() + ";");
	}

	@Test
	public void findListEntityResult_twoEntitiesPersisted_thenReturnsThePersistedEntities() {
		SomeJsonbEntity entity = new SomeJsonbEntity(1l, "otherIndexedValue");
		SomeJsonbEntity entity2 = new SomeJsonbEntity(2l, "anIndexedValue");
		String query = "SELECT * FROM test_table";
		connectorMock.whenExecuteQueryThenReturnResult(query, serialize(entity), serialize(entity2));

		List<SomeJsonbEntity> listEntityResult = entityManager.findListEntityResult(SomeJsonbEntity.class, query);

		Assertions.assertThat(listEntityResult).isNotEmpty();
		Assertions.assertThat(listEntityResult).containsExactly(entity, entity2);
		verify(connectorMock.getMock(), Mockito.times(1)).execute(anyString(), any(Consumer.class));
		verify(connectorMock.getMock(), Mockito.times(0)).executeOnWriteNode(anyString(), any(Consumer.class));
	}

	@Test
	public void findListEntityResultOnWriteNode_twoEntitiesPersisted_thenReturnsThePersistedEntities() {
		SomeJsonbEntity entity = new SomeJsonbEntity(1l, "otherIndexedValue");
		SomeJsonbEntity entity2 = new SomeJsonbEntity(2l, "anIndexedValue");
		String query = "SELECT * FROM test_table";
		connectorMock.whenExecuteOnWritingNodeQueryThenReturnResult(query, serialize(entity), serialize(entity2));

		List<SomeJsonbEntity> listEntityResult = entityManager.findListEntityResultOnWriteNode(SomeJsonbEntity.class, query);

		Assertions.assertThat(listEntityResult).isNotEmpty();
		Assertions.assertThat(listEntityResult).containsExactly(entity, entity2);
		verify(connectorMock.getMock(), Mockito.times(0)).execute(anyString(), any(Consumer.class));
		verify(connectorMock.getMock(), Mockito.times(1)).executeOnWriteNode(anyString(), any(Consumer.class));
	}

	@Test
	public void delete_queryForDeletionIsCreatedCorrectly() {
		entityManager.delete(SomeJsonbEntity.class, 3l);

		connectorMock.verfyExecute("delete from test_table where id=3");
	}

	private String serialize(SomeJsonbEntity entity) {
		return executeOrRuntime(() -> objectMapper.writeValueAsString(entity));
	}
}

















