package com.wafflestudio.webgam.global.common.model

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeTraceLazyDeletedEntity (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0,

    @CreatedDate
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    @CreatedBy
    open var createdBy: String = "",

    @LastModifiedDate
    open var modifiedAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedBy
    open var modifiedBy: String = "",

    open var isDeleted: Boolean = false,
) {
    abstract fun delete()
}