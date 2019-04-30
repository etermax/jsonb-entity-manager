package com.etermax.jsonb.orm.exceptions;

public interface ThrowableSupplier<T> {

	public T supply() throws Exception;
}
