package com.etermax.jsonb.orm;

import static com.etermax.jsonb.orm.TableNamesResolver.getSequenceName;
import static com.etermax.jsonb.orm.TableNamesResolver.getTableName;
import static com.etermax.jsonb.orm.exceptions.ExceptionCatcher.executeOrRuntime;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("rawtypes")
public class JsonbEntityManager {
	private static final Logger logger = LoggerFactory.getLogger(JsonbEntityManager.class);

	private ObjectMapper objectMapper;

	public JsonbEntityManager(ObjectMapper objectMapper, PostgresConnector connector) {
		this.objectMapper = objectMapper;
		this.connector = connector;
	}

	private PostgresConnector connector;

	public String serialize(JsonbEntity entity) {
		return executeOrRuntime(() -> objectMapper.writeValueAsString(entity).replace("'", "''"));
	}

	public void save(JsonbEntity entity) {
		if (!entity.persisted()) {
			assignIdToNewEntity(entity);
			String saveQuery = "INSERT INTO %s (id, entity) VALUES (%s, '%s');";
			execute(format(saveQuery, getTableName(entity.getClass()), entity.getId(), serialize(entity)));
		} else {
			String updateQuery = "update %s set entity= '%s' where id = %s;";
			execute(format(updateQuery, getTableName(entity.getClass()), serialize(entity), entity.getId()));
		}
	}

	public void execute(String query) {
		connector.execute(query);
	}

	protected <T> List<T> executeListEntityResultOnWriteNode(Class<T> clazz, String query) {
		List<String> jsons = newArrayList();
		connector.executeOnWriteNode(query, rs -> {
			executeOrRuntime(() -> {
				while (rs.next()) {
					jsons.add(rs.getString("entity"));
				}
			});
		});
		return jsons.stream().map(json -> deserialize(clazz, json)).collect(toList());
	}

	public <T> List<T> executeListEntityResult(Class<T> clazz, String query) {
		List<String> jsons = newArrayList();
		connector.execute(query, rs -> {
			executeOrRuntime(() -> {
				while (rs.next()) {
					jsons.add(rs.getString("entity"));
				}
			});
		});
		return jsons.stream().map(json -> deserialize(clazz, json)).collect(toList());
	}

	private void assignIdToNewEntity(JsonbEntity entity) {
		connector.executeNextVal(format("select nextval('%s')", getSequenceName(entity.getClass())), rs -> executeOrRuntime(() -> {
			rs.next();
			entity.setId(rs.getLong(1));
		}));
	}

	public <T> T executeUniqueResult(String query) {
		Retriever<T> retriever = new Retriever<>();
		connector.execute(query, rs -> {
			executeOrRuntime(() -> {
				if (rs.next()) {
					retriever.set((T) rs.getObject(1));
				}
			});
		});
		return retriever.get();
	}

	protected <T> T executeUniqueEntityResultOnWriteNode(Class<T> clazz, String query) {
		Retriever<String> retriever = new Retriever<>();
		connector.executeOnWriteNode(query, rs -> {
			executeOrRuntime(() -> {
				if (rs.next()) {
					retriever.set(rs.getString("entity"));
				}
			});
		});
		return retriever.get() == null ? null : deserialize(clazz, retriever.get());
	}

	public List executeListResult(String query) {
		List<Object> list = newArrayList();
		connector.execute(query, rs -> {
			executeOrRuntime(() -> {
				while (rs.next()) {
					list.add(rs.getObject(1));
				}
			});
		});
		return list;
	}

	public void delete(Class<?> clazz, Long id) {
		execute(format("delete from %s where id=%d", getTableName(clazz), id));

	}

	public <T> T findBy(Class<T> clazz, long id) {
		String saveQuery = "select id, entity from %s where id = %d;";
		try {
			return executeUniqueEntityResult(clazz, format(saveQuery, getTableName(clazz), id));
		} catch (Exception e) {
			logger.error("error connecting postgres", e);
			return null;
		}
	}

	public <T> T findByCode(Class<T> clazz, String code) {
		String saveQuery = "select id, entity from %s where entity->'code' = '\"%s\"'";
		try {
			return executeUniqueEntityResult(clazz, format(saveQuery, getTableName(clazz), code));
		} catch (Exception e) {
			logger.error("error connecting postgres", e);
			return null;
		}
	}

	public <T> List<T> findAll(Class<T> clazz) {
		String query = "SELECT * FROM %s;";
		return executeListEntityResult(clazz, format(query, getTableName(clazz)));
	}

	public <T> T executeUniqueEntityResult(Class<T> clazz, String query) {
		Retriever<String> retriever = new Retriever<>();
		connector.execute(query, rs -> {
			executeOrRuntime(() -> {
				if (rs.next()) {
					retriever.set(rs.getString("entity"));
				}
			});
		});
		return retriever.get() == null ? null : deserialize(clazz, retriever.get());
	}

	public <T> T deserialize(Class<T> clazz, String json) {
		return executeOrRuntime(() -> objectMapper.readValue(json, clazz));
	}

	public class Retriever<J> {
		private J element;

		public J get() {
			return element;
		}

		public void set(J element) {
			this.element = element;
		}

	}

	public PostgresConnector getConnector() {
		return connector;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void deleteFile(Long id) {
		connector.execute("delete from bytes_files where id = " + id);
	}

}
