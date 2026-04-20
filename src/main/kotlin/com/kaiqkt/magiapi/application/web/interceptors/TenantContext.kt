package com.kaiqkt.magiapi.application.web.interceptors


object TenantContext {
    private val currentTenant = ThreadLocal<String>()

    fun setTenant(tenant: String) {
        currentTenant.set(tenant)
    }

    fun getTenant(): String = currentTenant.get()

    fun clear() {
        currentTenant.remove()
    }
}