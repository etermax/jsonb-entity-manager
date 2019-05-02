package com.etermax.jsonb;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collection;

public class PostgresInitializer {

	private PostgresConnector connector;

	private TableNamesResolver tableNamesResolver;

	public PostgresInitializer(PostgresConnector connector, TableNamesResolver tableNamesResolver) {
		this.connector = connector;
		this.tableNamesResolver = tableNamesResolver;
	}

	public void initialize() {
		Collection<String> tableNames = getTableNames();
		tableNames.forEach(this::createTable);
		createByteArraysTable();
	}

	private void createByteArraysTable() {
		String queryExistTable = "SELECT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE  TABLE_NAME = 'bytes_files')";
		String query = "CREATE TABLE bytes_files (id bigint, name text, b bytea, CONSTRAINT bytes_files_pkey PRIMARY KEY (id)) ";
		connector.executeExist(queryExistTable, exist -> {
			if (!exist) {
				connector.execute(query);
			}
		});
	}

	private void createTable(String tableName) {
		String queryExistTable = format("SELECT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE  TABLE_NAME = '%s')", tableName);
		String query = format("CREATE TABLE %s (id bigint, entity jsonb, CONSTRAINT %s_pkey PRIMARY KEY (id)) ", tableName, tableName);
		connector.executeExist(queryExistTable, exist -> {
			if (!exist) {
				connector.execute(query);
				createSequence(getSequenceName(tableName));
			}
		});
		createIndexes(tableName);
	}

	private void createSequence(String sequenceName) {
		String queryExistSequence = format("SELECT EXISTS (SELECT 0 FROM pg_class where relname = '%s')", sequenceName);
		String query = format("CREATE SEQUENCE %s INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1", sequenceName);
		connector.executeExist(queryExistSequence, exist -> {
			if (!exist) {
				connector.execute(query);
			}
		});
	}

	private void createIndexes(String tableName) {
		String[] indexes = getIndexes(tableName);
		for (int i = 0; i < indexes.length; i++) {
			String idx = getIndexes(tableName)[i];
			if (isNotBlank(idx)) {
				String queryExistSequence = format("SELECT EXISTS (SELECT 0 FROM pg_indexes where tablename = '%s' and indexname = '%s')", tableName,
						"idx_" + tableName + i);
				String query = format("CREATE INDEX %s ON %s USING GIN ((entity %s))", "idx_" + tableName + i, tableName, idx);
				connector.executeExist(queryExistSequence, exist -> {
					if (!exist) {
						connector.execute(query);
					}
				});
			}
		}
	}

	private Collection<String> getTableNames() {
		return tableNamesResolver.getTableNames();
	}

	private String[] getIndexes(String tableName) {
		return tableNamesResolver.getIndexes(tableName);
	}

	private String getSequenceName(String tableName) {
		return tableNamesResolver.getSequenceName(tableName);
	}
}
