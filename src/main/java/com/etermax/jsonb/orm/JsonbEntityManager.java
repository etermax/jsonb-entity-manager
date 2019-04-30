package com.etermax.jsonb.orm;

import static com.etermax.jsonb.orm.exceptions.ExceptionCatcher.executeOrRuntime;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("rawtypes")
public class JsonbEntityManager {
	private static final Logger logger = LoggerFactory.getLogger(JsonbEntityManager.class);
	private ObjectMapper objectMapper;
	private PostgresConnector connector;
	private TableNamesResolver tableNamesResolver;

	public JsonbEntityManager(ObjectMapper objectMapper, PostgresConnector connector, TableNamesResolver tableNamesResolver) {
		this.objectMapper = objectMapper;
		this.connector = connector;
		this.tableNamesResolver = tableNamesResolver;
	}

	/**
	 * <p>Persist the entity. If it is new, then it will assign an id reading it from de corresponding sequence:
	 * <br>(the annotated table name with the prefix "seq_")
	 * </p>
	 *
	 * @param entity the JsonbEntity to persist
	 * @return the list with all matching objects
	 */
	public void save(JsonbEntity entity) {
		if (!entity.persisted()) {
			assignIdToNewEntity(entity);
			String saveQuery = "INSERT INTO %s (id, entity) VALUES (%s, '%s');";
			connector.execute(format(saveQuery, getTableName(entity.getClass()), entity.getId(), serialize(entity)));
		} else {
			String updateQuery = "update %s set entity= '%s' where id = %s;";
			connector.execute(format(updateQuery, getTableName(entity.getClass()), serialize(entity), entity.getId()));
		}
	}

	/**
	 * <p>Finds a list of entities for the query.
	 * </p>
	 *
	 * @param clazz The class of the objecct that will be deleted
	 * @param query the query to retrieve the JsonbEntities
	 * @return the list with all matching objects
	 */
	public <T> List<T> findListEntityResult(Class<T> clazz, String query) {
		List<String> jsons = newArrayList();
		connector.execute(query, resultSet -> executeOrRuntime(() -> {
			while (resultSet.next()) {
				jsons.add(resultSet.getString("entity"));
			}
		}));
		return jsons.stream().map(json -> deserialize(clazz, json)).collect(toList());
	}

	/**
	 * <p>Finds a list of entities for the query on the writing node. <br>
	 * Use it just when you can't wait for the propagation time over the reading nodes, but be careful about the load.
	 * </p>
	 *
	 * @param clazz The class of the objecct that will be deleted
	 * @param query the query to retrieve the JsonbEntities
	 * @return the list with all matching objects
	 */
	public <T> List<T> findListEntityResultOnWriteNode(Class<T> clazz, String query) {
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

	/**
	 * <p>Deletes from data base the persisted instance with that id
	 * </p>
	 *
	 * @param clazz The class of the objecct that will be deleted
	 * @param id    the assigned id on save
	 */
	public void delete(Class<?> clazz, Long id) {
		connector.execute(format("delete from %s where id=%d", getTableName(clazz), id));
	}

	/**
	 * <p>This method search a unique JsonbEntity.
	 * </p>
	 *
	 * @param clazz The class that will be deserialized to the parametrized type
	 * @param query The query to find a unique instance for your object
	 * @return one unique result for the class
	 */
	public <T> T findUniqueEntityResult(Class<T> clazz, String query) {
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

	/**
	 * <p>This method search a unique JsonbEntity on the writing node. <br>
	 * Use it just when you can't wait for the propagation time over the reading nodes, but be careful about the load.
	 * </p>
	 *
	 * @param clazz The class that will be deserialized to the parametrized type
	 * @param query The query to find a unique instance for your object
	 * @return one unique result for the class
	 */
	public <T extends JsonbEntity> T findUniqueEntityResultOnWriteNode(Class<T> clazz, String query) {
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

	/**
	 * <p>This method retrieves all the entities for the parametrized class
	 * </p>
	 *
	 * @param clazz The class that will be deserialized to the parametrized type
	 * @return the list with all persisted objects
	 */
	public <T> List<T> findAll(Class<T> clazz) {
		String query = "SELECT * FROM %s;";
		return findListEntityResult(clazz, format(query, getTableName(clazz)));
	}

	/**
	 * <p>Finds a persisted instance by the assigned id
	 * </p>
	 *
	 * @param clazz The class that will be deserialized to the parametrized type
	 * @param id    the assigned id on save
	 * @return an optional of the object. It will be empty if the id is wrong
	 */
	public <T> Optional<T> findById(Class<T> clazz, long id) {
		String saveQuery = "select id, entity from %s where id = %d;";
		return ofNullable(findUniqueEntityResult(clazz, format(saveQuery, getTableName(clazz), id)));
	}

	public <T> T findPrimitiveResult(String query) {
		Retriever<T> retriever = new Retriever<>();
		connector.execute(query, resultSet -> executeOrRuntime(() -> {
			if (resultSet.next()) {
				retriever.set((T) resultSet.getObject(1));
			}
		}));
		return retriever.get();
	}

	public List findPrimitiveListResult(String query) {
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

	private <T> T deserialize(Class<T> clazz, String json) {
		return executeOrRuntime(() -> objectMapper.readValue(json, clazz));
	}

	private void assignIdToNewEntity(JsonbEntity entity) {
		connector.executeNextVal(format("select nextval('%s')", getSequenceName(entity.getClass())), rs -> executeOrRuntime(() -> {
			rs.next();
			entity.setId(rs.getLong(1));
		}));
	}

	private String serialize(JsonbEntity entity) {
		return executeOrRuntime(() -> objectMapper.writeValueAsString(entity).replace("'", "''"));
	}

	private String getTableName(Class<?> clazz) {
		return tableNamesResolver.getTableName(clazz);
	}

	private String getSequenceName(Class<?> clazz) {
		return tableNamesResolver.getSequenceName(clazz);
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

}
