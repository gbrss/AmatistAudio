package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val bpm: Int,
    val updatedAt: Long = System.currentTimeMillis(),
    val jsonPayload: String
)
