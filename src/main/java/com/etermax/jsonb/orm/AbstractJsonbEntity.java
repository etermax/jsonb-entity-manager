package com.etermax.jsonb.orm;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class AbstractJsonbEntity extends AbstractEntity<Long> {

	private Long id;
	private String code;

	@Override
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setIdString(String id) {
		if (id != null) {
			this.id = Long.valueOf(id);
		}
	}

	@Override
	public void persist() {
		JsonbEntityManager.save(this);
	}

	@Override
	public void delete() {
		JsonbEntityManager.delete(this.getClass(), this.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		AbstractJsonbEntity rhs = (AbstractJsonbEntity) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}

	public String getCode() {
		if (code == null) {
			code = getIdString();
		}
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Boolean persisted() {
		return id != null && id > 0;
	}

}
