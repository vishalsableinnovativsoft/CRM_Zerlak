package com.startica.privateapp.multitenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private static final Logger log = LoggerFactory.getLogger(TenantIdentifierResolver.class);
    private static final String DEFAULT_TENANT = "crusher"; // fallback db

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getTenant(); // Fixed method name
        String resolved = (tenantId != null) ? tenantId : DEFAULT_TENANT;
        log.debug("[TenantIdentifierResolver] Resolved tenant: {} (raw ctx: {})", resolved, tenantId);
        return resolved;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
