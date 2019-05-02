package com.etermax.jsonb.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.etermax.jsonb.JsonbEntity;
import com.etermax.jsonb.tablenames.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;

@TableName(value = "test_table", indexes = { " -> 'indexedValue'" })
public class SomeJsonbEntity implements JsonbEntity {

	@JsonProperty
	private Long id;

	@JsonProperty
	private String indexedValue;

	public SomeJsonbEntity() {

	}

	public SomeJsonbEntity(Long id, String indexedValue) {
		this.id = id;
		this.indexedValue = indexedValue;
	}

	public SomeJsonbEntity(String indexedValue) {
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

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
