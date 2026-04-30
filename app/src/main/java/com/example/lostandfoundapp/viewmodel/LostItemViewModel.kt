package com.example.lostandfoundapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostandfoundapp.data.AppDatabase
import com.example.lostandfoundapp.model.ItemFilter
import com.example.lostandfoundapp.model.ItemStatus
import com.example.lostandfoundapp.model.ItemType
import com.example.lostandfoundapp.model.LostItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LostItemViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).lostItemDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(ItemFilter.ALL)
    val filter = _filter.asStateFlow()

    val approvedItems: StateFlow<List<LostItem>> = combine(_searchQuery, _filter) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        val baseFlow = if (query.isEmpty()) {
            dao.getItemsByStatus(ItemStatus.APPROVED)
        } else {
            dao.searchApprovedItems(query)
        }
        baseFlow.map { items ->
            when (filter) {
                ItemFilter.ALL -> items
                ItemFilter.LOST -> items.filter { it.type == ItemType.LOST }
                ItemFilter.FOUND -> items.filter { it.type == ItemType.FOUND }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingItems: StateFlow<List<LostItem>> = dao.getItemsByStatus(ItemStatus.PENDING)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMyReports(email: String): Flow<List<LostItem>> {
        return dao.getItemsByReporterAndStatuses(email, listOf(ItemStatus.PENDING, ItemStatus.REJECTED))
    }

    fun getItem(id: String): Flow<LostItem?> = dao.getItemById(id)

    fun addItem(item: LostItem) {
        viewModelScope.launch {
            dao.insertItem(item)
        }
    }

    fun approveItem(id: String) {
        viewModelScope.launch {
            dao.updateItemStatus(id, ItemStatus.APPROVED)
        }
    }

    fun rejectItem(id: String) {
        viewModelScope.launch {
            dao.updateItemStatus(id, ItemStatus.REJECTED)
        }
    }

    fun deleteItem(item: LostItem) {
        viewModelScope.launch {
            dao.deleteItem(item.id)
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterChange(newFilter: ItemFilter) {
        _filter.value = newFilter
    }
}
