package com.startica.privateapp.multitenant;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class TenantInterceptor implements StatementInspector {

    @Override
    public String inspect(String sql) {
        // Modify SQL queries for multi-tenancy if needed
        return sql; // Return the original or modified SQL
    }
}