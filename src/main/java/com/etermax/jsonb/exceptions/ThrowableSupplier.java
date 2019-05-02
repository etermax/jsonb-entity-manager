package com.etermax.jsonb.exceptions;

public interface ThrowableSupplier<T> {

	public T supply() throws Exception;
}
