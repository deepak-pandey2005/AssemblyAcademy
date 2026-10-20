package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    // Completed Lessons Queries
    @Query("SELECT * FROM completed_lessons")
    fun getCompletedLessons(): Flow<List<CompletedLesson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedLesson(lesson: CompletedLesson)

    // User Programs Queries
    @Query("SELECT * FROM user_programs ORDER BY updatedAt DESC")
    fun getUserPrograms(): Flow<List<UserProgram>>

    @Query("SELECT * FROM user_programs WHERE id = :id")
    suspend fun getProgramById(id: Int): UserProgram?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProgram(program: UserProgram)

    @Query("DELETE FROM user_programs WHERE id = :id")
    suspend fun deleteUserProgramById(id: Int)

    // User Stats Queries
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    @Query("UPDATE user_stats SET totalPoints = totalPoints + :points WHERE id = 1")
    suspend fun addPoints(points: Int)

    @Query("UPDATE user_stats SET instructionsExecuted = instructionsExecuted + :count WHERE id = 1")
    suspend fun addInstructionsCount(count: Int)
}
