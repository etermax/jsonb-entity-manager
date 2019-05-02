package com.etermax.jsonb;

import static java.lang.String.format;

import org.junit.Before;
import org.junit.Test;

import com.etermax.jsonb.mocks.PostgresConnectorStub;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PostgresInitializerTest {
	private PostgresConnectorStub connectorMock;
	private ObjectMapper objectMapper;
	private TableNamesResolver tableNamesResolver;
	private PostgresInitializer postgresInitializer;

	@Before
	public void setUp() {
		TableNamesResolver tableNamesResolver = new TableNamesResolver("com.etermax.jsonb");
		connectorMock = new PostgresConnectorStub();
		objectMapper = new ObjectMapper();
		postgresInitializer = new PostgresInitializer(connectorMock.getMock(), tableNamesResolver);
	}

	@Test
	public void initialize_emptydatabase_scenario() {
		String queryExistTable = givenNoTablesExists();
		String sequenceString = givenNoSequencesExists();
		String sequencesString = givenNoIndexesExists();

		postgresInitializer.initialize();

		thenTablesAreCreated(queryExistTable);
		andSequencesAreCreated(queryExistTable, sequenceString);
		andIndexesAreCreated(sequencesString);
	}

	@Test
	public void initialize_existentTableExists_skipTableAndSequenceCreation() {
		String queryExistTable = givenTestTableExists();
		String sequenceString = givenNoSequencesExists();
		String sequencesString = givenNoIndexesExists();

		postgresInitializer.initialize();

		thenTestTableIsSkippedFromCreation(queryExistTable);
		andSequenceOverTestTableIsSkipped(queryExistTable, sequenceString);
		andIndexesAreCreated(sequencesString);
	}

	@Test
	public void initialize_sequenceExists_thenSequenceIsSkipped() {
		String queryExistTable = givenNoTablesExists();
		String sequenceString1 = givenOtherTableSequenceExists();
		String sequenceString = sequenceString1;
		String sequencesString = givenNoIndexesExists();

		postgresInitializer.initialize();

		thenTablesAreCreated(queryExistTable);
		andOtherTableSequenceCreationIsSkipped(queryExistTable, sequenceString);
		andIndexesAreCreated(sequencesString);
	}

	@Test
	public void initialize_indexExists_thenIndexIsSipped() {
		String queryExistTable = givenNoTablesExists();
		String sequenceString = givenNoSequencesExists();
		String sequencesString = givenFirstIndexOnOtherTableExists();

		postgresInitializer.initialize();

		thenTablesAreCreated(queryExistTable);
		andSequencesAreCreated(queryExistTable, sequenceString);
		andCreationOfFirstIndexOnTheOtherTableIsSkipped(sequencesString);
	}

	private void andCreationOfFirstIndexOnTheOtherTableIsSkipped(String sequencesString) {
		connectorMock.assertThatQueryWasExecuted(format(sequencesString, "test_table", "idx_test_table0"));
		connectorMock.assertThatQueryWasExecuted(format(sequencesString, "test_table", "idx_test_table0"));
		connectorMock.assertThatQueryWasExecuted(format(sequencesString, "other_test_table", "idx_other_test_table0"));
		connectorMock.assertThatQueryWasExecuted("CREATE INDEX idx_test_table0 ON test_table USING GIN ((entity  -> 'indexedValue'))");
		connectorMock
				.assertThatQueryWasNotExecuted("CREATE INDEX idx_other_test_table0 ON other_test_table USING GIN ((entity  -> 'indexedValue1'))");
		connectorMock.assertThatQueryWasExecuted("CREATE INDEX idx_other_test_table1 ON other_test_table USING GIN ((entity  -> 'indexedValue2'))");
	}

	private String givenFirstIndexOnOtherTableExists() {
		String sequencesString1 = "SELECT EXISTS (SELECT 0 FROM pg_indexes where tablename = '%s' and indexname = '%s')";
		connectorMock.whenExecuteQueryThenReturnResult(format(sequencesString1, "test_table", "idx_test_table0"), false);
		connectorMock.whenExecuteQueryThenReturnResult(format(sequencesString1, "other_test_table", "idx_other_test_table0"), true);
		connectorMock.whenExecuteQueryThenReturnResult(format(sequencesString1, "other_test_table", "idx_other_test_table1"), false);
		return sequencesString1;
	}

	private void andOtherTableSequenceCreationIsSkipped(String queryExistTable, String sequenceString) {
		String createTestTable = "CREATE SEQUENCE seq_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		String createOtherTestTable = "CREATE SEQUENCE seq_other_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		connectorMock.assertThatQueryWasExecuted(format(sequenceString, "seq_test_table"));
		connectorMock.assertThatQueryWasExecuted(format(queryExistTable, "other_test_table"));
		connectorMock.assertThatQueryWasExecuted(createTestTable);
		connectorMock.assertThatQueryWasNotExecuted(createOtherTestTable);
	}

	private String givenOtherTableSequenceExists() {
		String sequenceString1 = "SELECT EXISTS (SELECT 0 FROM pg_class where relname = '%s')";
		connectorMock.whenExecuteQueryThenReturnResult(format(sequenceString1, "seq_test_table"), false);
		connectorMock.whenExecuteQueryThenReturnResult(format(sequenceString1, "seq_other_test_table"), true);
		return sequenceString1;
	}

	private void thenTestTableIsSkippedFromCreation(String queryExistTable) {
		String createTestTable = "CREATE SEQUENCE seq_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		String createOtherTestTable = "CREATE SEQUENCE seq_other_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		connectorMock.assertThatQueryWasExecuted(format(queryExistTable, "test_table"));
		connectorMock.assertThatQueryWasExecuted(format(queryExistTable, "other_test_table"));
		connectorMock.assertThatQueryWasNotExecuted(createTestTable);
		connectorMock.assertThatQueryWasExecuted(createOtherTestTable);
	}

	private String givenTestTableExists() {
		String queryExistTable = "SELECT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE  TABLE_NAME = '%s')";
		connectorMock.whenExecuteQueryThenReturnResult(format(queryExistTable, "test_table"), true);
		connectorMock.whenExecuteQueryThenReturnResult(format(queryExistTable, "other_test_table"), false);
		return queryExistTable;
	}

	private void andIndexesAreCreated(String sequencesString) {
		connectorMock.assertThatQueryWasExecuted(format(sequencesString, "test_table", "idx_test_table0"));
		connectorMock.assertThatQueryWasExecuted(format(sequencesString, "test_table", "idx_test_table0"));
		connectorMock.assertThatQueryWasExecuted(format(sequencesString, "other_test_table", "idx_other_test_table0"));
		connectorMock.assertThatQueryWasExecuted("CREATE INDEX idx_test_table0 ON test_table USING GIN ((entity  -> 'indexedValue'))");
		connectorMock.assertThatQueryWasExecuted("CREATE INDEX idx_other_test_table0 ON other_test_table USING GIN ((entity  -> 'indexedValue1'))");
		connectorMock.assertThatQueryWasExecuted("CREATE INDEX idx_other_test_table1 ON other_test_table USING GIN ((entity  -> 'indexedValue2'))");
	}

	private void andSequenceOverTestTableIsSkipped(String queryExistTable, String sequenceString) {
		String createTestTable = "CREATE SEQUENCE seq_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		String createOtherTestTable = "CREATE SEQUENCE seq_other_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		connectorMock.assertThatQueryWasNotExecuted(format(sequenceString, "seq_test_table"));
		connectorMock.assertThatQueryWasExecuted(format(queryExistTable, "other_test_table"));
		connectorMock.assertThatQueryWasNotExecuted(createTestTable);
		connectorMock.assertThatQueryWasExecuted(createOtherTestTable);
	}

	private void andSequencesAreCreated(String queryExistTable, String sequenceString) {
		String createTestTable = "CREATE SEQUENCE seq_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		String createOtherTestTable = "CREATE SEQUENCE seq_other_test_table INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1";
		connectorMock.assertThatQueryWasExecuted(format(sequenceString, "seq_test_table"));
		connectorMock.assertThatQueryWasExecuted(format(queryExistTable, "other_test_table"));
		connectorMock.assertThatQueryWasExecuted(createTestTable);
		connectorMock.assertThatQueryWasExecuted(createOtherTestTable);
	}

	private void thenTablesAreCreated(String queryExistTable) {
		String createTestTable = "CREATE TABLE test_table (id bigint, entity jsonb, CONSTRAINT test_table_pkey PRIMARY KEY (id)) ";
		String createOtherTestTable = "CREATE TABLE other_test_table (id bigint, entity jsonb, CONSTRAINT other_test_table_pkey PRIMARY KEY (id)) ";
		connectorMock.assertThatQueryWasExecuted(format(queryExistTable, "test_table"));
		connectorMock.assertThatQueryWasExecuted(format(queryExistTable, "other_test_table"));
		connectorMock.assertThatQueryWasExecuted(createTestTable);
		connectorMock.assertThatQueryWasExecuted(createOtherTestTable);
	}

	private String givenNoIndexesExists() {
		String sequencesString = "SELECT EXISTS (SELECT 0 FROM pg_indexes where tablename = '%s' and indexname = '%s')";
		connectorMock.whenExecuteQueryThenReturnResult(format(sequencesString, "test_table", "idx_test_table0"), false);
		connectorMock.whenExecuteQueryThenReturnResult(format(sequencesString, "other_test_table", "idx_other_test_table0"), false);
		connectorMock.whenExecuteQueryThenReturnResult(format(sequencesString, "other_test_table", "idx_other_test_table1"), false);
		return sequencesString;
	}

	private String givenNoSequencesExists() {
		String sequenceString = "SELECT EXISTS (SELECT 0 FROM pg_class where relname = '%s')";
		connectorMock.whenExecuteQueryThenReturnResult(format(sequenceString, "seq_test_table"), false);
		connectorMock.whenExecuteQueryThenReturnResult(format(sequenceString, "seq_other_test_table"), false);
		return sequenceString;
	}

	private String givenNoTablesExists() {
		String queryExistTable = "SELECT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE  TABLE_NAME = '%s')";
		connectorMock.whenExecuteQueryThenReturnResult(format(queryExistTable, "test_table"), false);
		connectorMock.whenExecuteQueryThenReturnResult(format(queryExistTable, "other_test_table"), false);
		return queryExistTable;
	}
}
