package com.ruslan.terminalssh.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_commands",
    foreignKeys = [ForeignKey(
        entity = ConnectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["connectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["connectionId"])]
)
data class FavoriteCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val connectionId: Long,
    val command: String,
    val description: String?,
    val createdAt: Long
)
