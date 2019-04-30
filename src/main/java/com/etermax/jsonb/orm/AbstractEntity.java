package com.etermax.jsonb.orm;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;

public abstract class AbstractEntity<T extends Serializable> {

	public abstract void persist();

	public abstract void delete();

	public abstract T getId();

	public String getIdString() {
		return getId() != null ? getId().toString() : null;
	}

	@Override
	public boolean equals(Object obj) {
		if (AbstractEntity.class.isAssignableFrom(obj.getClass())) {
			return EqualsBuilder.reflectionEquals(getId(), ((AbstractEntity<?>) obj).getId());
		}
		return super.equals(obj);
	}
}
