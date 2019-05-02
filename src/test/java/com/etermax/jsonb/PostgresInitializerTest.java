package com.etermax.jsonb;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.etermax.jsonb.mocks.PostgresConnectorStub;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PostgresInitializerTest {
	private PostgresConnectorStub connectorMock;
	private ObjectMapper objectMapper;
	private TableNamesResolver tableNamesResolver;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TableNamesResolver tableNamesResolver = new TableNamesResolver("com.etermax.jsonb.orm");
		connectorMock = new PostgresConnectorStub();
		objectMapper = new ObjectMapper();
	}

	@Test
	public void test() {

	}
}
