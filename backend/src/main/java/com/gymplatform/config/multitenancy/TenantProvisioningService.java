package com.gymplatform.config.multitenancy;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final DataSource dataSource;

    public void provisionTenant(String tenantId) {
        String schemaName = "tenant_" + tenantId;
        log.info("Provisioning schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            }

            connection.createStatement()
                    .execute("SET search_path TO " + schemaName);

            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schemaName);
            database.setLiquibaseSchemaName(schemaName);

            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-tenant.yaml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update("");

            log.info("Schema {} provisioned successfully", schemaName);
        } catch (Exception e) {
            log.error("Failed to provision tenant schema: {}", schemaName, e);
            throw new RuntimeException("Failed to provision tenant: " + tenantId, e);
        }
    }
}
