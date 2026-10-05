package com.example.sms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Parent of every entity. Spring fills these 4 columns automatically
 * (see JpaAuditingConfig + AuditorAwareImpl) - nobody sets them by hand.
 *
 * @MappedSuperclass = no table of its own; its columns are added to the child tables.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    // set once, when the row is first inserted
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // email of the logged-in user who created the row ("anonymous" for public registration)
    @CreatedBy
    @Column(updatable = false, length = 100)
    private String createdBy;

    // refreshed on every update
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(length = 100)
    private String updatedBy;
}
