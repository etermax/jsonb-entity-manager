package com.etermax.jsonb;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import com.etermax.jsonb.builder.HikariDataSourceBuilder;
import com.etermax.jsonb.connection.PostgresConnector;
import com.etermax.jsonb.domain.SomeJsonbEntity;
import com.etermax.jsonb.initializer.PostgresInitializer;
import com.etermax.jsonb.tablenames.TableNamesResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;

public class SimplePostgreSQLTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private PostgresConnector connector;
	private final TableNamesResolver tableNamesResolver = new TableNamesResolver("com.etermax.jsonb.domain");
	private JsonbEntityManager entityManager;

	@Test
	public void testSimple() throws SQLException {
		try (PostgreSQLContainer postgres = new PostgreSQLContainer<>()) {
			postgres.start();
			HikariDataSource hikariDataSource = initializeConnector(postgres);
			connector = new PostgresConnector(hikariDataSource, hikariDataSource);

			PostgresInitializer initializer = new PostgresInitializer(connector, tableNamesResolver);

			initializer.initialize();

			entityManager = new JsonbEntityManager(objectMapper, connector, tableNamesResolver);

			SomeJsonbEntity entity = new SomeJsonbEntity("test");

			entityManager.save(entity);
			assertThat(entity.getId() != null);

			Optional<SomeJsonbEntity> optionalEntity = entityManager.findById(SomeJsonbEntity.class, entity.getId());

			assertThat(optionalEntity).isPresent();
			assertThat(optionalEntity.get()).isEqualTo(entity);
		}

	}

	private HikariDataSource initializeConnector(PostgreSQLContainer container) {
		return HikariDataSourceBuilder.defaultDataSource().withUrl(container.getJdbcUrl()).withPoolName("test").withUser(container.getUsername())
				.withPassword(container.getPassword()).build();
	}

}