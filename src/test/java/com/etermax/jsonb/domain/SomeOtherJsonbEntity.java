package com.etermax.jsonb.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.etermax.jsonb.JsonbEntity;
import com.etermax.jsonb.tablenames.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;

@TableName(value = "other_test_table", indexes = { " -> 'indexedValue1'", " -> 'indexedValue2'" })
public class SomeOtherJsonbEntity implements JsonbEntity {

	@JsonProperty
	private Long id;

	@JsonProperty
	private String indexedValue1;
	@JsonProperty
	private String indexedValue2;

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
