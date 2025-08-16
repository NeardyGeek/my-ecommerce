package com.codebase.microservices.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableCassandraRepositories(basePackages = "com.codebase.microservices.orderservice.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspaceName;

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    @Value("${spring.cassandra.schema-action}")
    private String schemaAction;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.valueOf(schemaAction.toUpperCase());
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        return Arrays.asList(
                CreateKeyspaceSpecification.createKeyspace(keyspaceName)
                        .ifNotExists()
                        .with(KeyspaceOption.DURABLE_WRITES, true)
                        .withSimpleReplication(1)
        );
    }

    @Override
    protected List<String> getStartupScripts() {
        return Arrays.asList(
                // Create UDT for order_item
                "CREATE TYPE IF NOT EXISTS " + keyspaceName + ".order_item (" +
                        "    item_id text," +
                        "    item_name text," +
                        "    upc text," +
                        "    quantity int," +
                        "    unit_price decimal," +
                        "    total_price decimal," +
                        "    item_picture_url text" +
                        ");",

                // Create orders table
                "CREATE TABLE IF NOT EXISTS " + keyspaceName + ".orders (" +
                        "    order_id uuid PRIMARY KEY," +
                        "    customer_id text," +
                        "    customer_email text," +
                        "    order_status text," +
                        "    total_amount decimal," +
                        "    currency text," +
                        "    shipping_address text," +
                        "    billing_address text," +
                        "    payment_method text," +
                        "    order_items list<frozen<order_item>>," +
                        "    created_at timestamp," +
                        "    updated_at timestamp," +
                        "    payment_id text," +
                        "    notes text" +
                        ");",

                // Create secondary indexes for queries
                "CREATE INDEX IF NOT EXISTS ON " + keyspaceName + ".orders (customer_id);",
                "CREATE INDEX IF NOT EXISTS ON " + keyspaceName + ".orders (customer_email);",
                "CREATE INDEX IF NOT EXISTS ON " + keyspaceName + ".orders (order_status);",
                "CREATE INDEX IF NOT EXISTS ON " + keyspaceName + ".orders (payment_id);"
        );
    }
}