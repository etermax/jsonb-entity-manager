package com.etermax.jsonb.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) // can use in method only.
public @interface TableName {
	public String value() default "";

	public String[] indexes() default { "" };
}
