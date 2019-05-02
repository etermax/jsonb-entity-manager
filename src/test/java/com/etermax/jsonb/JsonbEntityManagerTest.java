package com.etermax.jsonb;

import static com.etermax.jsonb.exceptions.ExceptionCatcher.executeOrRuntime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.etermax.jsonb.domain.SomeJsonbEntity;
import com.etermax.jsonb.mocks.PostgresConnectorStub;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonbEntityManagerTest {

	private JsonbEntityManager entityManager;
	private PostgresConnectorStub connectorMock;
	private static long nextSequenceVal = 1L;
	private ObjectMapper objectMapper;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TableNamesResolver tableNamesResolver = new TableNamesResolver("com.etermax.jsonb");
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
		String query = "SELECT * FROM test_table where id > 13";
		connectorMock.whenExecuteQueryThenReturnResult(query, serialize(entity), serialize(entity2));

		List<SomeJsonbEntity> listEntityResult = entityManager.findListEntityResult(SomeJsonbEntity.class, query);

		assertThat(listEntityResult).isNotEmpty();
		assertThat(listEntityResult).containsExactly(entity, entity2);
		verify(connectorMock.getMock(), Mockito.times(1)).execute(anyString(), any(Consumer.class));
		verify(connectorMock.getMock(), Mockito.times(0)).executeOnWriteNode(anyString(), any(Consumer.class));
	}

	@Test
	public void findAll_callTheFindListWithTheCorrectSelect() {
		SomeJsonbEntity entity = new SomeJsonbEntity(1l, "otherIndexedValue");
		SomeJsonbEntity entity2 = new SomeJsonbEntity(2l, "anIndexedValue");
		connectorMock.whenExecuteQueryThenReturnResult("SELECT * FROM test_table;", serialize(entity), serialize(entity2));

		List<SomeJsonbEntity> listEntityResult = entityManager.findAll(SomeJsonbEntity.class);

		assertThat(listEntityResult).isNotEmpty();
		assertThat(listEntityResult).containsExactly(entity, entity2);
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

		assertThat(listEntityResult).isNotEmpty();
		assertThat(listEntityResult).containsExactly(entity, entity2);
		verify(connectorMock.getMock(), Mockito.times(0)).execute(anyString(), any(Consumer.class));
		verify(connectorMock.getMock(), Mockito.times(1)).executeOnWriteNode(anyString(), any(Consumer.class));
	}

	@Test
	public void delete_queryForDeletionIsCreatedCorrectly() {
		entityManager.delete(SomeJsonbEntity.class, 3l);

		connectorMock.verfyExecute("delete from test_table where id=3");
	}

	@Test
	public void findUniqueEntityResult_existentEntityReturnsObject() {
		SomeJsonbEntity entity = new SomeJsonbEntity(1l, "otherIndexedValue");
		String query = "SELECT * FROM test_table where id = 1";
		connectorMock.whenExecuteQueryThenReturnResult(query, serialize(entity));

		Optional<SomeJsonbEntity> entityResult = entityManager.findUniqueEntityResult(SomeJsonbEntity.class, query);

		assertThat(entityResult).isPresent();
		assertThat(entityResult.get()).isEqualTo(entity);
		verify(connectorMock.getMock(), Mockito.times(1)).execute(anyString(), any(Consumer.class));
	}

	@Test
	public void findById_findsUniqueEntityResult() {
		SomeJsonbEntity entity = new SomeJsonbEntity(1l, "otherIndexedValue");
		connectorMock.whenExecuteQueryThenReturnResult("select id, entity from test_table where id = 1;", serialize(entity));

		Optional<SomeJsonbEntity> entityResult = entityManager.findById(SomeJsonbEntity.class, 1L);

		assertThat(entityResult).isPresent();
		assertThat(entityResult.get()).isEqualTo(entity);
		verify(connectorMock.getMock(), Mockito.times(1)).execute(anyString(), any(Consumer.class));
	}

	@Test
	public void findPrimitiveResult_countSample_returnsASimpleString() {
		String query = "select count(*) from test_table where id > 1;";
		connectorMock.whenExecuteQueryThenReturnResult(query, 33L);

		Long count = entityManager.findPrimitiveResult(query);

		assertThat(count).isNotNull();
		assertThat(count).isEqualTo(33L);
		verify(connectorMock.getMock(), Mockito.times(1)).execute(anyString(), any(Consumer.class));
	}

	@Test
	public void findPrimitiveListResult_selectIdsSample() {
		String query = "select id from test_table where id > 1;";
		connectorMock.whenExecuteQueryThenReturnResult(query, 12L, 11L, 344L);

		List<Long> ids = entityManager.findPrimitiveListResult(query);

		assertThat(ids).isNotEmpty();
		assertThat(ids).containsExactly(12L, 11L, 344L);
		verify(connectorMock.getMock(), Mockito.times(1)).execute(anyString(), any(Consumer.class));
	}

	@Test
	public void findUniqueEntityResult_nonExistentEntityReturnsObject() {
		String query = "SELECT * FROM test_table where id = 1";
		connectorMock.whenExecuteQueryThenReturnResult(query);

		Optional<SomeJsonbEntity> entityResult = entityManager.findUniqueEntityResult(SomeJsonbEntity.class, query);

		assertThat(entityResult).isNotPresent();
		verify(connectorMock.getMock(), Mockito.times(1)).execute(anyString(), any(Consumer.class));
	}

	@Test
	public void findUniqueEntityResultOnWriteNode_existentEntityReturnsObject() {
		SomeJsonbEntity entity = new SomeJsonbEntity(1l, "otherIndexedValue");
		String query = "SELECT * FROM test_table where id = 1";
		connectorMock.whenExecuteOnWritingNodeQueryThenReturnResult(query, serialize(entity));

		Optional<SomeJsonbEntity> entityResult = entityManager.findUniqueEntityResultOnWriteNode(SomeJsonbEntity.class, query);

		assertThat(entityResult).isPresent();
		assertThat(entityResult.get()).isEqualTo(entity);
		verify(connectorMock.getMock(), Mockito.times(1)).executeOnWriteNode(anyString(), any(Consumer.class));
	}

	@Test
	public void findUniqueEntityResultOnWriteNode_nonExistentEntityReturnsObject() {
		String query = "SELECT * FROM test_table where id = 1";
		connectorMock.whenExecuteOnWritingNodeQueryThenReturnResult(query);

		Optional<SomeJsonbEntity> entityResult = entityManager.findUniqueEntityResultOnWriteNode(SomeJsonbEntity.class, query);

		assertThat(entityResult).isNotPresent();
		verify(connectorMock.getMock(), Mockito.times(1)).executeOnWriteNode(anyString(), any(Consumer.class));
	}

	private String serialize(SomeJsonbEntity entity) {
		return executeOrRuntime(() -> objectMapper.writeValueAsString(entity));
	}
}

















