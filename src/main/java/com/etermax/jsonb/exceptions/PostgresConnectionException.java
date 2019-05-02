package com.etermax.jsonb.exceptions;

public class PostgresConnectionException extends RuntimeException {

	public PostgresConnectionException(Exception e) {
		super(e);
	}

	public PostgresConnectionException() {
		super();
	}
}
