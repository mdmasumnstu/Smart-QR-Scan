package com.example.data.repository

import com.example.data.local.ScanDao
import com.example.data.model.ScanItem
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanItem>> = scanDao.getAllScans()
    val favoriteScans: Flow<List<ScanItem>> = scanDao.getFavoriteScans()
    val recentScans: Flow<List<ScanItem>> = scanDao.getRecentScans(5)

    fun searchScans(query: String): Flow<List<ScanItem>> = scanDao.searchScans(query)

    suspend fun insert(item: ScanItem): Long = scanDao.insertScan(item)

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        scanDao.setFavorite(id, !currentFavorite)
    }

    suspend fun deleteById(id: Long) = scanDao.deleteScanById(id)

    suspend fun clearAll() = scanDao.clearAll()
}
