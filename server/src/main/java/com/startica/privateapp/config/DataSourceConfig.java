package com.startica.privateapp.config;

import com.startica.privateapp.multitenant.MultiTenantConnectionProviderImpl;
import com.startica.privateapp.multitenant.TenantRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import com.startica.privateapp.multitenant.TenantInterceptor;
import com.startica.privateapp.multitenant.TenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder; // Ensure this import is present
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.stonecrusher.repositories",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class DataSourceConfig {

    @Autowired
    @Lazy
    private TenantRoutingDataSource tenantRoutingDataSource;

    private final DataSourceProperties dataSourceProperties;

    @Value("${multitenant.datasource.jdbc-url-template:jdbc:mysql://localhost:3306/%s}")
    private String tenantJdbcUrlTemplate;

    @Value("${multitenant.datasource.username:root}")
    private String tenantDbUsername;

    @Value("${multitenant.datasource.password:root}")
    private String tenantDbPassword;

    @Value("${multitenant.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String tenantDriverClassName;

    public DataSourceConfig(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Bean(name = "defaultDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource defaultDataSource() {
        return dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "tenantRoutingDataSource")
    public TenantRoutingDataSource tenantRoutingDataSource(
            @Qualifier("defaultDataSource") DataSource defaultDataSource) {
        TenantRoutingDataSource tenantRoutingDataSource = new TenantRoutingDataSource();
        tenantRoutingDataSource.setDefaultTargetDataSource(defaultDataSource);
        tenantRoutingDataSource.setTargetDataSources(new HashMap<>());
        tenantRoutingDataSource.afterPropertiesSet();
        return tenantRoutingDataSource;
    }

    @Bean
    public MultiTenantConnectionProviderImpl multiTenantConnectionProvider(
            @Qualifier("tenantRoutingDataSource") TenantRoutingDataSource tenantRoutingDataSource) {
        MultiTenantConnectionProviderImpl provider = new MultiTenantConnectionProviderImpl();
        provider.setTenantRoutingDataSource(tenantRoutingDataSource);
        provider.setJdbcUrlTemplate(tenantJdbcUrlTemplate);
        provider.setDbUsername(tenantDbUsername);
        provider.setDbPassword(tenantDbPassword);
        provider.setDriverClassName(tenantDriverClassName);
        return provider;
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("tenantRoutingDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder,
            MultiTenantConnectionProviderImpl multiTenantConnectionProvider,
            TenantIdentifierResolver tenantIdentifierResolver) {
        HashMap<String, Object> jpaProperties = new HashMap<>();
        // Use DATABASE strategy to route to separate tenant databases
        jpaProperties.put("hibernate.multiTenancy", "DATABASE"); // Hibernate 5/6
        jpaProperties.put("hibernate.multi_tenancy", "DATABASE"); // Ensure compatibility
        jpaProperties.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider);
        jpaProperties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);
        // Helpful to see SQL per tenant
        jpaProperties.put("hibernate.session_factory.statement_inspector", TenantInterceptor.class.getName());
        return builder
                .dataSource(dataSource)
                .packages("com.stonecrusher.model")
                .persistenceUnit("default")
                .properties(jpaProperties)
                .build();
    }

    @Bean(name = "transactionManager")
    public JpaTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactoryBean.getObject());
        return transactionManager;
    }

    public void addUserDataSource(String schemaName) {
        HikariDataSource userDataSource = new HikariDataSource();
        userDataSource.setJdbcUrl(String.format(tenantJdbcUrlTemplate, schemaName));
        userDataSource.setUsername(tenantDbUsername);
        userDataSource.setPassword(tenantDbPassword);
        userDataSource.setDriverClassName(tenantDriverClassName);

        tenantRoutingDataSource.addDataSource(schemaName, userDataSource);
    }
}
