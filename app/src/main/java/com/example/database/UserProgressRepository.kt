package com.example.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class UserProgressRepository(private val dao: UserProgressDao) {
    val completedLessons: Flow<List<CompletedLesson>> = dao.getCompletedLessons()
    val userPrograms: Flow<List<UserProgram>> = dao.getUserPrograms()
    val userStats: Flow<UserStats?> = dao.getUserStats()

    suspend fun getProgramById(id: Int): UserProgram? {
        return dao.getProgramById(id)
    }

    suspend fun completeLesson(topicId: String, points: Int) {
        // Mark lesson completed
        dao.insertCompletedLesson(CompletedLesson(topicId, System.currentTimeMillis(), points))
        
        // Ensure stats exist, and add points
        val currentStats = userStats.firstOrNull() ?: UserStats()
        val newPoints = currentStats.totalPoints + points
        
        // Update streak logic (simplified for learning context)
        val now = System.currentTimeMillis()
        var streak = currentStats.streakDays
        if (currentStats.lastActiveTimestamp == 0L) {
            streak = 1
        } else {
            val diffMs = now - currentStats.lastActiveTimestamp
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            if (diffDays == 1L) {
                streak += 1
            } else if (diffDays > 1L) {
                streak = 1
            }
        }
        
        dao.insertUserStats(currentStats.copy(
            totalPoints = newPoints,
            streakDays = streak,
            lastActiveTimestamp = now
        ))
    }

    suspend fun saveProgram(program: UserProgram) {
        dao.insertUserProgram(program)
        
        // Update stats count
        val currentStats = userStats.firstOrNull() ?: UserStats()
        if (program.id == 0) {
            dao.insertUserStats(currentStats.copy(
                programsCreated = currentStats.programsCreated + 1,
                lastActiveTimestamp = System.currentTimeMillis()
            ))
        }
    }

    suspend fun deleteProgram(id: Int) {
        dao.deleteUserProgramById(id)
    }

    suspend fun updateStats(stats: UserStats) {
        dao.insertUserStats(stats)
    }

    suspend fun recordExecution(count: Int) {
        // Ensure stats exist
        val currentStats = userStats.firstOrNull() ?: UserStats()
        dao.insertUserStats(currentStats.copy(
            instructionsExecuted = currentStats.instructionsExecuted + count
        ))
    }
}
