package com.etermax.jsonb.orm;

public interface JsonbEntity {

	Long getId();

	void setId(Long id);

	default Boolean persisted() {
		return getId() != null && getId() > 0;
	}

}
