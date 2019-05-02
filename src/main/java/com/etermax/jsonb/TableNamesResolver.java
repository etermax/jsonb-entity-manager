package com.etermax.jsonb;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

public class TableNamesResolver {

	public static final String SEQUENCE_PREFIX = "seq_";
	private static final String MISSING_TABLE_NAME =
			"class name needed for persist entity, use TableName annotation and register the package in " + "TablesNamesResolver";

	private Map<Class<?>, String> tableNames = newHashMap();
	private Map<String, String[]> indexesNames = newHashMap();

	public TableNamesResolver(String... packagesToScan) {
		Reflections reflections = new Reflections(packagesToScan);
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(TableName.class);
		for (Class<?> classes : annotated) {
			TableName tableName = classes.getAnnotation(TableName.class);
			tableNames.put(classes, tableName.value());
			indexesNames.put(tableName.value(), tableName.indexes());
		}
	}

	public String getTableName(Class<?> clazz) {
		String tableName = tableNames.get(clazz);
		if (StringUtils.isBlank(tableName)) {
			throw new RuntimeException(MISSING_TABLE_NAME);
		}
		return tableName;
	}

	public String getSequenceName(Class<?> clazz) {
		return SEQUENCE_PREFIX + getTableName(clazz);
	}

	public String getSequenceName(String tableName) {
		return SEQUENCE_PREFIX + tableName;
	}

	public String[] getIndexes(String tableName) {
		return indexesNames.get(tableName);
	}

	public Collection<String> getTableNames() {
		return tableNames.values();
	}

}
