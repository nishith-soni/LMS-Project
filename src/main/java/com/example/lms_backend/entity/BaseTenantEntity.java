package com.example.lms_backend.entity;

import com.example.lms_backend.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Getter
@Setter
@MappedSuperclass
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = String.class)
)
@Filter(
        name = "tenantFilter",
        condition = "tenant_id = :tenantId"
)
public abstract class BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @PrePersist
    public void assignTenant() {
        if (this.tenantId == null) {
            String current = TenantContext.getTenantId();
            if (current == null || current.isBlank()) {
                throw new IllegalStateException("Cannot persist entity: No tenant-id set in context!");
            }
            this.tenantId = current;
        }
    }
}
