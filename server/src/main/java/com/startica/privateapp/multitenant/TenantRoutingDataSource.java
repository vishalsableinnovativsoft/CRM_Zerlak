package com.startica.privateapp.multitenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> dataSources = new HashMap<>();
    private final Map<String, DataSource> dataSourceRegistry = new HashMap<>(); // Your separate registry

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenant();
    }

    public synchronized void addDataSource(String tenant, DataSource dataSource) {
        if (!dataSources.containsKey(tenant)) {
            dataSources.put(tenant, dataSource);
            dataSourceRegistry.put(tenant, dataSource); // Add to your registry
            Map<Object, Object> resolved = new HashMap<>(getResolvedDataSources());
            resolved.put(tenant, dataSource);
            super.setTargetDataSources(resolved);
            super.afterPropertiesSet();
        }
    }

    public boolean containsDataSource(String tenant) {
        return dataSources.containsKey(tenant);
    }

    public boolean existsDataSource(String tenant) {
        return containsDataSource(tenant);
    }

    public DataSource getDataSource(String tenant) {
        return dataSourceRegistry.get(tenant);
    }
}