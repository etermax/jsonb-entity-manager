package com.etermax.jsonb.exceptions;

import java.util.function.Consumer;
import java.util.function.Function;

public class ExceptionCatcher {
	private ExceptionCatcher() {
	}

	public static <E> E executeOrRuntime(ThrowableSupplier<E> supplier) {
		return executeOrRuntime(supplier, RuntimeException.class);
	}

	public static void executeOrRuntime(ThrowableConsumer consumer) {
		executeOrRuntime(consumer, RuntimeException.class);
	}

	public static <E> E executeOrRuntime(ThrowableSupplier<E> supplier, Class<? extends RuntimeException> exceptionClass) {
		return executeOrRuntime(supplier, exceptionClass, "");
	}

	public static void executeOrRuntime(ThrowableConsumer consumer, Class<? extends RuntimeException> exceptionClass) {
		executeOrRuntime(consumer, exceptionClass, "");
	}

	public static <E> E executeOrRuntime(ThrowableSupplier<E> supplier, Class<? extends RuntimeException> exceptionClass, String message) {
		try {
			return supplier.supply();
		} catch (Exception e) {
			try {
				throw exceptionClass.getConstructor(Exception.class).newInstance(e);
			} catch (Exception e1) {
				throw new RuntimeException(message, e1);
			}
		}
	}

	public static void executeOrRuntime(ThrowableConsumer consumer, Class<? extends RuntimeException> exceptionClass, String message) {
		try {
			consumer.execute();
		} catch (Exception e) {
			try {
				throw exceptionClass.getConstructor(Exception.class).newInstance(e);
			} catch (Exception e1) {
				throw new RuntimeException(message, e1);
			}
		}
	}

	public static void executeOrElse(ThrowableConsumer consumer, Consumer<Exception> exceptionConsumer) {
		try {
			consumer.execute();
		} catch (Exception e) {
			exceptionConsumer.accept(e);
		}
	}

	public static <E> E executeWithFallback(ThrowableSupplier<E> supplier, Function<Exception, E> fallback) {
		try {
			return supplier.supply();
		} catch (Exception e) {
			return fallback.apply(e);
		}
	}
}
