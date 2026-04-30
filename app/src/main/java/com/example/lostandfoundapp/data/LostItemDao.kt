package com.example.lostandfoundapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lostandfoundapp.model.ItemStatus
import com.example.lostandfoundapp.model.LostItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LostItemDao {
    @Query("SELECT * FROM lost_items ORDER BY date DESC")
    fun getAllItems(): Flow<List<LostItem>>

    @Query("SELECT * FROM lost_items WHERE status = :status ORDER BY date DESC")
    fun getItemsByStatus(status: ItemStatus): Flow<List<LostItem>>

    @Query("SELECT * FROM lost_items WHERE reporterEmail = :email AND status IN (:statuses) ORDER BY date DESC")
    fun getItemsByReporterAndStatuses(email: String, statuses: List<ItemStatus>): Flow<List<LostItem>>

    @Query("SELECT * FROM lost_items WHERE status = 'APPROVED' AND (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%')")
    fun searchApprovedItems(searchQuery: String): Flow<List<LostItem>>

    @Query("SELECT * FROM lost_items WHERE id = :id")
    fun getItemById(id: String): Flow<LostItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LostItem)

    @Query("UPDATE lost_items SET status = :status WHERE id = :id")
    suspend fun updateItemStatus(id: String, status: ItemStatus)

    @Query("DELETE FROM lost_items WHERE id = :id")
    suspend fun deleteItem(id: String)
}
