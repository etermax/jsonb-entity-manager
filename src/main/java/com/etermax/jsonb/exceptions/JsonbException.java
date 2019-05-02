package com.etermax.jsonb.exceptions;

public class JsonbException extends RuntimeException {
	public JsonbException(String message, Exception e1) {
		super(message, e1);
	}

	public JsonbException(String message) {
		super(message);
	}
}
