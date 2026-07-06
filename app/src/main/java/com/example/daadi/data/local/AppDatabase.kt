package com.example.daadi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_users")
data class CachedUser(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val role: String,
    val createdAt: String,
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
    val coins: Int,
    val xp: Int,
    val rating: Int,
    val isBanned: Boolean,
    val isVerified: Boolean
)

@Dao
interface UserDao {
    @Query("SELECT * FROM cached_users ORDER BY rating DESC")
    fun getAllUsers(): Flow<List<CachedUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<CachedUser>)

    @Query("DELETE FROM cached_users")
    suspend fun deleteAll()
}

@Entity(tableName = "cached_matches")
data class CachedMatch(
    @PrimaryKey val id: String,
    val hostName: String,
    val opponentName: String,
    val status: String,
    val winner: String?,
    val movesCount: Int,
    val createdAt: String
)

@Dao
interface MatchDao {
    @Query("SELECT * FROM cached_matches ORDER BY createdAt DESC")
    fun getAllMatches(): Flow<List<CachedMatch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<CachedMatch>)

    @Query("DELETE FROM cached_matches")
    suspend fun deleteAll()
}

@Database(entities = [CachedUser::class, CachedMatch::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun matchDao(): MatchDao
}
