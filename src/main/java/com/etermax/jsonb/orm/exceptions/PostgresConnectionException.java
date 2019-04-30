package com.etermax.jsonb.orm.exceptions;

public class PostgresConnectionException extends RuntimeException {

	public PostgresConnectionException(Exception e) {
		super(e);
	}
}
