package com.mafauser.service.example

import com.mafauser.service.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(name = "examples")
class Example(
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(length = 1024)
    var description: String? = null,
    @Version
    @Column(nullable = false)
    var version: Long = 0,
    id: UUID = UUID.randomUUID(),
) : BaseEntity(id = id) {
    override fun toString(): String = "Example(id=$id, name=$name)"
}
