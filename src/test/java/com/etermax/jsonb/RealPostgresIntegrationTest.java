package com.etermax.jsonb;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import com.etermax.jsonb.builder.HikariDataSourceBuilder;
import com.etermax.jsonb.connection.PostgresConnector;
import com.etermax.jsonb.domain.SomeJsonbEntity;
import com.etermax.jsonb.domain.SomeOtherJsonbEntity;
import com.etermax.jsonb.initializer.PostgresInitializer;
import com.etermax.jsonb.tablenames.TableNamesResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;

public class RealPostgresIntegrationTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final TableNamesResolver tableNamesResolver = new TableNamesResolver("com.etermax.jsonb.domain");
	private static JsonbEntityManager entityManager;
	private static PostgresConnector connector;

	@BeforeClass
	public static void setUpPostgres() {
		final Optional<String> fromCompose = Optional.ofNullable(System.getenv("FROM_COMPOSE"));

		if(fromCompose.isPresent()){
			HikariDataSource hikariDataSource = HikariDataSourceBuilder.defaultDataSource().withUrl("jdbc:postgresql://postgres:5432/testdb").withPoolName("test")
					.withUser("postgres").withPassword("postgres").build();
			connector = new PostgresConnector(hikariDataSource);

		}else{
			PostgreSQLContainer postgres = new PostgreSQLContainer<>();
			postgres.start();
			HikariDataSource hikariDataSource = HikariDataSourceBuilder.defaultDataSource().withUrl(postgres.getJdbcUrl()).withPoolName("test")
					.withUser(postgres.getUsername()).withPassword(postgres.getPassword()).build();
			connector = new PostgresConnector(hikariDataSource);

		}

				new PostgresInitializer(connector, tableNamesResolver).initialize();
		entityManager = new JsonbEntityManager(objectMapper, connector, tableNamesResolver);
	}

	@Before
	public void setUp() {
		connector.execute("truncate test_table");
		connector.execute("truncate other_test_table");
	}

	@Test
	public void whenSavingAnEntityThenItIsRetrievedCorrectly() {
		SomeJsonbEntity entity = new SomeJsonbEntity("test");

		entityManager.save(entity);

		assertThat(entity.getId()).isNotNull();

		Optional<SomeJsonbEntity> optionalEntity = entityManager.findById(SomeJsonbEntity.class, entity.getId());

		assertThat(optionalEntity).isPresent();
		assertThat(optionalEntity.get()).isEqualTo(entity);

	}

	@Test
	public void retrieveInexistentIdReturnsEmtpyOptional() {

		assertThat(entityManager.findById(SomeOtherJsonbEntity.class, 22L)).isNotPresent();
	}

	@Test
	public void whenSavingMultipleInstancesFindAllRetrievesIt() {
		SomeJsonbEntity entity = new SomeJsonbEntity("test");
		SomeJsonbEntity entity2 = new SomeJsonbEntity("test2");
		SomeJsonbEntity entity3 = new SomeJsonbEntity("test");

		SomeOtherJsonbEntity entityThatShouldntBeRetrieved = new SomeOtherJsonbEntity();

		entityManager.save(entity);
		entityManager.save(entity2);
		entityManager.save(entity3);
		entityManager.save(entityThatShouldntBeRetrieved);

		List<SomeJsonbEntity> allEntities = entityManager.findAll(SomeJsonbEntity.class);

		assertThat(allEntities).isNotEmpty();
		assertThat(allEntities).containsExactly(entity, entity2, entity3);

	}

	@Test
	public void whenQueryingByIndexWithExistentEntityReturnsObject() {
		SomeJsonbEntity entity = new SomeJsonbEntity("test");
		SomeJsonbEntity entity2 = new SomeJsonbEntity("test2");
		SomeJsonbEntity entity3 = new SomeJsonbEntity("test");

		entityManager.save(entity);
		entityManager.save(entity2);
		entityManager.save(entity3);

		Optional<SomeJsonbEntity> result = entityManager.findUniqueEntityResult(SomeJsonbEntity.class,
				format("select * from %s where entity->'indexedValue' @> '\"test2\"'", tableNamesResolver.getTableName(SomeJsonbEntity.class)));

		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(entity2);
	}

	@Test
	public void whenQueryingByIndexWithMoreThanOneExistentEntityReturnsList() {
		SomeJsonbEntity entity = new SomeJsonbEntity("test");
		SomeJsonbEntity entity2 = new SomeJsonbEntity("test2");
		SomeJsonbEntity entity3 = new SomeJsonbEntity("test");

		SomeOtherJsonbEntity entityThatShouldntBeRetrieved = new SomeOtherJsonbEntity();

		entityManager.save(entity);
		entityManager.save(entity2);
		entityManager.save(entity3);
		entityManager.save(entityThatShouldntBeRetrieved);

		List<SomeJsonbEntity> result = entityManager.findListEntityResult(SomeJsonbEntity.class,
				format("select * from %s where entity->'indexedValue' @> '\"test\"'", tableNamesResolver.getTableName(SomeJsonbEntity.class)));

		assertThat(result).isNotEmpty();
		assertThat(result).containsExactly(entity, entity3);
	}

	@Test
	public void whenFindPrimitiveResultReturnsTheResult() {
		SomeJsonbEntity entity = new SomeJsonbEntity("test");
		SomeJsonbEntity entity2 = new SomeJsonbEntity("test2");
		SomeJsonbEntity entity3 = new SomeJsonbEntity("test");

		entityManager.save(entity);
		entityManager.save(entity2);
		entityManager.save(entity3);

		Long result = entityManager.findPrimitiveResult(
				format("select count(*) from %s where entity->'indexedValue' @> '\"test\"'", tableNamesResolver.getTableName(SomeJsonbEntity.class)));

		assertThat(result).isEqualTo(2L);

	}

	@Test
	public void whenFindPrimitiveListReturnsTheCorrectList() {
		SomeJsonbEntity entity = new SomeJsonbEntity("test");
		SomeJsonbEntity entity2 = new SomeJsonbEntity("test2");
		SomeJsonbEntity entity3 = new SomeJsonbEntity("test");

		entityManager.save(entity);
		entityManager.save(entity2);
		entityManager.save(entity3);

		List<String> result = entityManager.findPrimitiveListResult(
				format("select entity->>'indexedValue' from %s where entity->'indexedValue' " + "@> '\"test\"'",
						tableNamesResolver.getTableName(SomeJsonbEntity.class)));

		assertThat(result).isNotEmpty();
		assertThat(result).containsExactly("test", "test");

	}

	@Test
	public void whenDeleteAnEntityItWillNotBeInTheDatabaseAnymore() {
		SomeJsonbEntity entity = new SomeJsonbEntity("test");

		entityManager.save(entity);

		assertThat(entity.getId()).isNotNull();

		entityManager.delete(SomeJsonbEntity.class, entity.getId());

		assertThat(entityManager.findById(SomeJsonbEntity.class, entity.getId())).isNotPresent();
	}
}