package com.etermax.jsonb;

import com.etermax.jsonb.connection.PostgresConnector;
import com.etermax.jsonb.tablenames.TableNamesResolver;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static com.etermax.jsonb.exceptions.ExceptionCatcher.executeOrRuntime;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("rawtypes")
public class JsonbEntityManager {
    public static final String ENTITY_FIELD = "entity";
    private ObjectMapper objectMapper;
    private PostgresConnector connector;
    private TableNamesResolver tableNamesResolver;

    public JsonbEntityManager(ObjectMapper objectMapper, PostgresConnector connector, TableNamesResolver tableNamesResolver) {
        this.objectMapper = objectMapper;
        this.connector = connector;
        this.tableNamesResolver = tableNamesResolver;
    }

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

    public <T extends JsonbEntity> List<T> findListEntityResult(Class<T> clazz, String query) {
        List<String> jsons = newArrayList();
        connector.execute(query, resultSet -> executeOrRuntime(() -> {
            while (resultSet.next()) {
                jsons.add(resultSet.getString(ENTITY_FIELD));
            }
        }));
        return jsons.stream().map(json -> deserialize(clazz, json)).collect(toList());
    }

    public <T extends JsonbEntity> List<T> findListEntityResultOnWriteNode(Class<T> clazz, String query) {
        List<String> jsons = newArrayList();
        connector.executeOnWriteNode(query, rs -> executeOrRuntime(() -> {
            while (rs.next()) {
                jsons.add(rs.getString(ENTITY_FIELD));
            }
        }));
        return jsons.stream().map(json -> deserialize(clazz, json)).collect(toList());
    }

    public void delete(Class<?> clazz, Long id) {
        connector.execute(format("delete from %s where id=%d", getTableName(clazz), id));
    }

    public <T extends JsonbEntity> Optional<T> findUniqueEntityResult(Class<T> clazz, String query) {
        Retriever<String> retriever = new Retriever<>();
        connector.execute(query, rs -> executeOrRuntime(() -> {
            if (rs.next()) {
                retriever.set(rs.getString(ENTITY_FIELD));
            }
        }));
        return ofNullable(retriever.get()).map(element -> deserialize(clazz, element));
    }

    public <T extends JsonbEntity> Optional<T> findUniqueEntityResultOnWriteNode(Class<T> clazz, String query) {
        Retriever<String> retriever = new Retriever<>();
        connector.executeOnWriteNode(query, rs -> executeOrRuntime(() -> {
            if (rs.next()) {
                retriever.set(rs.getString(ENTITY_FIELD));
            }
        }));
        return ofNullable(retriever.get()).map(element -> deserialize(clazz, element));
    }

    public <T extends JsonbEntity> List<T> findAll(Class<T> clazz) {
        String query = "SELECT * FROM %s;";
        return findListEntityResult(clazz, format(query, getTableName(clazz)));
    }

    public <T extends JsonbEntity> Optional<T> findById(Class<T> clazz, long id) {
        String saveQuery = "select id, entity from %s where id = %d;";
        return findUniqueEntityResult(clazz, format(saveQuery, getTableName(clazz), id));
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

    public <T> List<T> findPrimitiveListResult(String query) {
        List<T> list = newArrayList();
        connector.execute(query, rs -> executeOrRuntime(() -> {
            while (rs.next()) {
                list.add((T) rs.getObject(1));
            }
        }));
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

    private class Retriever<J> {

        private J element;

        public J get() {
            return element;
        }

        public void set(J element) {
            this.element = element;
        }

    }

}
