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

	private static ObjectMapper objectMapper;
	private static PostgresConnector connector;

	public static void configObjectMapper(ObjectMapper objectMapper) {
		JsonbEntityManager.objectMapper = objectMapper;
	}

	public static void configPostgresConnector(PostgresConnector connector) {
	}

	public static String serialize(AbstractJsonbEntity entity) {
		return executeOrRuntime(() -> objectMapper.writeValueAsString(entity).replace("'", "''"));
	}

	public static void save(AbstractJsonbEntity entity) {
		if (!entity.persisted()) {
			assignIdToNewEntity(entity);
			String saveQuery = "INSERT INTO %s (id, entity) VALUES (%s, '%s');";
			execute(format(saveQuery, getTableName(entity.getClass()), entity.getId(), serialize(entity)));
		} else {
			String updateQuery = "update %s set entity= '%s' where id = %s;";
			execute(format(updateQuery, getTableName(entity.getClass()), serialize(entity), entity.getId()));
		}
	}

	public static void execute(String query) {
		connector.execute(query);
	}

	public static <T> List<T> executeListEntityResult(Class<T> clazz, String query) {
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

	private static void assignIdToNewEntity(AbstractJsonbEntity entity) {
		connector.execute(format("select nextval('%s')", getSequenceName(entity.getClass())), rs -> executeOrRuntime(() -> {
			rs.next();
			entity.setId(rs.getLong(1));
		}));
	}

	public static <T> T executeUniqueResult(String query) {
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

	public static List executeListResult(String query) {
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

	public static void delete(Class<?> clazz, Long id) {
		execute(format("delete from %s where id=%d", getTableName(clazz), id));

	}

	public static <T> T findBy(Class<T> clazz, long id) {
		String saveQuery = "select id, entity from %s where id = %d;";
		try {
			return executeUniqueEntityResult(clazz, format(saveQuery, getTableName(clazz), id));
		} catch (Exception e) {
			logger.error("error connecting postgres", e);
			return null;
		}
	}

	public static <T> T findByCode(Class<T> clazz, String code) {
		String saveQuery = "select id, entity from %s where entity->'code' = '\"%s\"'";
		try {
			return executeUniqueEntityResult(clazz, format(saveQuery, getTableName(clazz), code));
		} catch (Exception e) {
			logger.error("error connecting postgres", e);
			return null;
		}
	}

	public static <T> List<T> findAll(Class<T> clazz) {
		String query = "SELECT * FROM %s;";
		return executeListEntityResult(clazz, format(query, getTableName(clazz)));
	}

	public static <T> T executeUniqueEntityResult(Class<T> clazz, String query) {
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

	public static <T> T deserialize(Class<T> clazz, String json) {
		return executeOrRuntime(() -> objectMapper.readValue(json, clazz));
	}

	public static class Retriever<J> {
		private J element;

		public J get() {
			return element;
		}

		public void set(J element) {
			this.element = element;
		}

	}

	public static PostgresConnector getConnector() {
		return connector;
	}

	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public static void persistFile(Long id, String name, byte[] bytes) {
		deleteFile(id);
		connector.executeByteArrayInsert(id, name, bytes);
	}

	public static byte[] findFile(Long id) {
		return connector.executeFindByteArray(id);
	}

	public static void deleteFile(Long id) {
		connector.execute("delete from bytes_files where id = " + id);
	}

}
