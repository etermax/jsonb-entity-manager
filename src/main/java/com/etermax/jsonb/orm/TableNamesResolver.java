package com.etermax.jsonb.orm;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

public class TableNamesResolver {

	public static final String SEQUENCE_PREFIX = "seq_";
	private static final String MISSING_TABLE_NAME = "class name needed for persist entity, use TableName annotation and register the package in TablesNamesResolver";

	private static Map<Class<?>, String> tableNames = newHashMap();
	private static Map<String, String[]> indexesNames = newHashMap();

	static {
		Reflections reflections = new Reflections( //
				"com.rinlit.model.domain.jsonb");
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(TableName.class);
		for (Class<?> classes : annotated) {
			TableName tableName = classes.getAnnotation(TableName.class);
			tableNames.put(classes, tableName.value());
			indexesNames.put(tableName.value(), tableName.indexes());
		}
	}

	public static String getTableName(Class<?> clazz) {
		String tableName = tableNames.get(clazz);
		if (StringUtils.isBlank(tableName)) {
			throw new RuntimeException(MISSING_TABLE_NAME);
		}
		return tableName;
	}

	public static String getSequenceName(Class<?> clazz) {
		return SEQUENCE_PREFIX + getTableName(clazz);
	}

	public static String getSequenceName(String tableName) {
		return SEQUENCE_PREFIX + tableName;
	}

	public static String[] getIndexes(String tableName) {
		return indexesNames.get(tableName);
	}

	public static Collection<String> getTableNames() {
		return tableNames.values();
	}

}
