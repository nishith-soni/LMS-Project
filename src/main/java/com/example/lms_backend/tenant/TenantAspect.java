package com.example.lms_backend.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(2)
public class TenantAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@within(org.springframework.stereotype.Service)")
    public void enableTenantFilter(JoinPoint jointPoint) {

        String tenantId = TenantContext.getTenantId();

        if (tenantId != null && !tenantId.isBlank()) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        }
    }
}
