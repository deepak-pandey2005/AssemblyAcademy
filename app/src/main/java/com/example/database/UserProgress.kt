package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_lessons")
data class CompletedLesson(
    @PrimaryKey val topicId: String,
    val completedAt: Long = System.currentTimeMillis(),
    val pointsEarned: Int = 100
)

@Entity(tableName = "user_programs")
data class UserProgram(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val code: String,
    val architecture: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val totalPoints: Int = 0,
    val streakDays: Int = 0,
    val lastActiveTimestamp: Long = 0L,
    val instructionsExecuted: Int = 0,
    val programsCreated: Int = 0
)
