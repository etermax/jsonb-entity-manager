package com.etermax.jsonb.orm.domain;

import com.etermax.jsonb.orm.JsonbEntity;
import com.etermax.jsonb.orm.TableName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@TableName(value = "test_table", indexes = { " -> 'indexedValue'" })
public class SomeJsonbEntity implements JsonbEntity {

	@JsonProperty
	private Long id;

	@JsonProperty
	private String indexedValue;

	@JsonCreator
	public SomeJsonbEntity(@JsonProperty String indexedValue) {
		this.id = id;
		this.indexedValue = indexedValue;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}
}
