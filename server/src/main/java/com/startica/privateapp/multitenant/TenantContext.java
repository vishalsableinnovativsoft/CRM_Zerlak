package com.startica.privateapp.multitenant;

public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    // ✅ Set tenant for the current thread (usually based on JWT dbName)
    public static void setTenant(String tenant) {
        CURRENT_TENANT.set(tenant);
    }

    // ✅ Get current tenant
    public static String getTenant() {
        return CURRENT_TENANT.get();
    }

    // ✅ Clear tenant after request completes
    public static void clearTenant() {
        CURRENT_TENANT.remove();
    }
}
