package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DatasetDao {
    @Query("SELECT * FROM datasets ORDER BY timestamp DESC")
    fun getAllDatasets(): Flow<List<DatasetEntity>>

    @Query("SELECT * FROM datasets WHERE id = :id LIMIT 1")
    suspend fun getDatasetById(id: Long): DatasetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataset(dataset: DatasetEntity): Long

    @Query("DELETE FROM datasets WHERE id = :id")
    suspend fun deleteDataset(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE datasetId = :datasetId ORDER BY timestamp ASC")
    fun getMessagesForDataset(datasetId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE datasetId = :datasetId")
    suspend fun deleteMessagesForDataset(datasetId: Long)
}
