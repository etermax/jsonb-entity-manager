package com.etermax.jsonb.orm;

/*
public class PostgresBaseTest {

    private static final String TEST_FILE_NAME = "postgres-configuration-test.yml";
    protected static PostgresConnector connector;
    protected ObjectMapper objectMapper;

    @Before
    public synchronized void setUp() {
        setupLogger();
        setupObjectMapper();
        setupConnector();
        initializePostgres();
    }

    private void setupLogger() {
        Logger root = (Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ERROR);
    }

    private void setupObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private void setupConnector() {
        if (connector == null) {
            PostgresConfiguration configuration = readConfigurationFromFile();
            connector = new PostgresConnector(configuration, configuration);
        }
    }

    private PostgresConfiguration readConfigurationFromFile() {
        PostgresConfiguration configuration = load(PostgresConfiguration.class, TEST_FILE_NAME);

        String postgresUrl = System.getenv("POSTGRES_URL");
        if (StringUtils.isNotBlank(postgresUrl)) {
            configuration.setUrl(postgresUrl);
        }

        return configuration;
    }

    private void initializePostgres() {
        Collection<String> tableNames = getTableNames();
        PostgresInitializer initializer = new PostgresInitializer(connector);
        initializer.initialize();
        tableNames.forEach(t -> connector.execute("truncate " + t + " cascade"));
    }
}
*/