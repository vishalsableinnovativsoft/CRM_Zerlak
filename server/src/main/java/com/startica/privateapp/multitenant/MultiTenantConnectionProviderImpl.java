package com.startica.privateapp.multitenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {
    private static final Logger log = LoggerFactory.getLogger(MultiTenantConnectionProviderImpl.class);

    private TenantRoutingDataSource tenantRoutingDataSource;

    // Optional properties to allow on-the-fly DataSource creation for tenants
    private String jdbcUrlTemplate = "jdbc:mysql://localhost:3306/%s"; // template will be overridden via config
    private String dbUsername = "root";
    private String dbPassword = "root";
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    public MultiTenantConnectionProviderImpl() {
        // No-arg constructor for Hibernate
    }

    public void setTenantRoutingDataSource(TenantRoutingDataSource tenantRoutingDataSource) {
        this.tenantRoutingDataSource = tenantRoutingDataSource;
    }

    public void setDbUrlPrefix(String dbUrlPrefix) {
        this.jdbcUrlTemplate = dbUrlPrefix + "%s";
    }

    public void setJdbcUrlTemplate(String jdbcUrlTemplate) {
        this.jdbcUrlTemplate = jdbcUrlTemplate;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        // Fallback to the default (parent) datasource
        return tenantRoutingDataSource.getConnection();
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        String tenant = String.valueOf(tenantIdentifier);

        // Try to fetch an existing DataSource for this tenant
        DataSource ds = tenantRoutingDataSource.getDataSource(tenant);
        if (ds == null) {
            // If the tenant datasource doesn't exist, create and register it dynamically
            if (tenant == null || tenant.trim().isEmpty() || "crusher".equalsIgnoreCase(tenant)) {
                // Parent (default) tenant -> route to default datasource
                log.debug("[MTCP] Using default (parent) datasource for tenant: {}", tenant);
                return tenantRoutingDataSource.getConnection();
            }

            try {
                log.info("[MTCP] Creating DataSource on-the-fly for tenant: {}", tenant);
                com.zaxxer.hikari.HikariDataSource userDS = new com.zaxxer.hikari.HikariDataSource();
                userDS.setJdbcUrl(String.format(jdbcUrlTemplate, tenant));
                userDS.setUsername(dbUsername);
                userDS.setPassword(dbPassword);
                userDS.setDriverClassName(driverClassName);
                // Optional pool tuning similar to elsewhere in the app
                userDS.setConnectionTimeout(30000);
                userDS.setIdleTimeout(600000);
                userDS.setMaxLifetime(1800000);
                userDS.setMaximumPoolSize(10);

                tenantRoutingDataSource.addDataSource(tenant, userDS);
                ds = userDS;
                log.info("[MTCP] DataSource registered for tenant: {}", tenant);
            } catch (Exception e) {
                log.error("[MTCP] Failed creating DataSource for tenant {}. Falling back to default. Error: {}", tenant, e.getMessage(), e);
                return tenantRoutingDataSource.getConnection();
            }
        }
        return ds.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
