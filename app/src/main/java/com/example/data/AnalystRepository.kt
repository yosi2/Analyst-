package com.example.data

import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DatasetDao
import com.example.data.local.DatasetEntity
import kotlinx.coroutines.flow.Flow

class AnalystRepository(
    private val datasetDao: DatasetDao,
    private val chatMessageDao: ChatMessageDao
) {
    val allDatasets: Flow<List<DatasetEntity>> = datasetDao.getAllDatasets()

    suspend fun getDatasetById(id: Long): DatasetEntity? {
        return datasetDao.getDatasetById(id)
    }

    suspend fun insertDataset(dataset: DatasetEntity): Long {
        return datasetDao.insertDataset(dataset)
    }

    suspend fun deleteDataset(id: Long) {
        datasetDao.deleteDataset(id)
        chatMessageDao.deleteMessagesForDataset(id)
    }

    fun getMessagesForDataset(datasetId: Long): Flow<List<ChatMessageEntity>> {
        return chatMessageDao.getMessagesForDataset(datasetId)
    }

    suspend fun insertMessage(message: ChatMessageEntity) {
        chatMessageDao.insertMessage(message)
    }
}
