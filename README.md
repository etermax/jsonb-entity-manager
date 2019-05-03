# PostgreSQL Jsonb Entity Manager
Entity manager provides a simple way to persist and retrieve objects from postgres with a destructured schema. However it still has all the potential of SQL for querying.

## Getting Started

The entity manager provides you of an easy way to persist and retrieve objects serialized to postgres jsonb structure:
```
SomeJsonbEntity entity = new SomeJsonbEntity("test");

entityManager.save(entity);
entityManager.findById(SomeJsonbEntity.class, entity.getId())
entityManager.findAll(SomeJsonbEntity.class);

```

This manager also separates the read queries from the write ones transparently, so you can scale your postgres with reading and writting nodes. 

### Prerequisites

It is necesary to have a PostgreSQL server with version over 9.4 and a database created.
If you use the PostgresInitializer tool to create the databases, the user provided should have permissions to do that.
Java version should be 8 or grather

The class will need to be marked with the TableName annotation that provides the tablename and the indexes needed for the initializer to create the structure:

```

@TableName(value = "test_table", indexes = { " -> 'indexedValue'" })
public class SomeJsonbEntity implements JsonbEntity {

	@JsonProperty
	private Long id;

	@JsonProperty
	private String indexedValue;

	public SomeJsonbEntity() {

	}

	public SomeJsonbEntity(Long id, String indexedValue) {
		this.id = id;
		this.indexedValue = indexedValue;
	}

	public SomeJsonbEntity(String indexedValue) {
		this.indexedValue = indexedValue;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

}
		
```
The initializer then will usage the table name info to work
		
```
PostgresConnector connector = new PostgresConnector(readDataSource, writingDataSource);
PostgresInitializer initializer = new PostgresInitializer(connector, tableNamesResolver);
initializer.initialize();
		
```

### Installing

First add the dependency:
```
		<dependency>
			<groupId>com.etermax.jsonb.orm</groupId>
			<artifactId>jsonb-entity-manager</artifactId>
			<version>1.0-SNAPSHOT</version>
		</dependency>
```
or gradle
```
compile "com.etermax.jsonb.orm:jsonb-entity-manager:1.0-SNAPSHOT"

```
Then you will need to create  the HikariDataSource (we choose this because it allows to reset connections and scale your reading nodes behind the balancer)

```
HikariDataSource hikariDataSource = HikariDataSourceBuilder.defaultDataSource()
    .withUrl(postgres.getJdbcUrl())
    .withPoolName("test")
    .withUser(postgres.getUsername())
    .withPassword(postgres.getPassword())
    .build();
		
```

The next step is to create the Postgres connector and the TableNameResolver (this las one recieves the package to scan the annotated classes)
```
PostgresConnector connector = new PostgresConnector(readHikariDataSource, writegHikariDataSource);

TableNamesResolver tableNamesResolver = new TableNamesResolver("com.your.jsonb.entities.package");
```

With all this you can prepare and create your entity mannager 
```
JsonbEntityManager entityManager = new JsonbEntityManager(new ObjectMapper(), connector, tableNamesResolver);
```



## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.



## Authors

* **Billie Thompson** - *Initial work* - [PurpleBooth](https://github.com/PurpleBooth)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

