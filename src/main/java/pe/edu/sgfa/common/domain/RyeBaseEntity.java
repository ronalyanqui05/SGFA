package pe.edu.sgfa.common.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import pe.edu.sgfa.common.persistence.RyeInstantStringConverter;

@MappedSuperclass
public abstract class RyeBaseEntity extends RyeIdentifiableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    @Convert(converter = RyeInstantStringConverter.class)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Convert(converter = RyeInstantStringConverter.class)
    private Instant updatedAt;

    @PrePersist
    protected void ryeAlCrear() {
        Instant ahoraYnq = Instant.now();
        createdAt = ahoraYnq;
        updatedAt = ahoraYnq;
    }

    @PreUpdate
    protected void ryeAlActualizar() {
        updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAtYnq) {
        this.createdAt = createdAtYnq;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAtYnq) {
        this.updatedAt = updatedAtYnq;
    }
}
