package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scan_items ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scan_items WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scan_items ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScans(limit: Int = 5): Flow<List<ScanItem>>

    @Query("""
        SELECT * FROM scan_items 
        WHERE rawValue LIKE '%' || :query || '%' 
           OR displayTitle LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchScans(query: String): Flow<List<ScanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(item: ScanItem): Long

    @Update
    suspend fun updateScan(item: ScanItem)

    @Query("UPDATE scan_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM scan_items WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scan_items")
    suspend fun clearAll()
}
