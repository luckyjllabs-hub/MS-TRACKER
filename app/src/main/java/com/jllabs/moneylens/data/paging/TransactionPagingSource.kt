package com.jllabs.moneylens.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jllabs.moneylens.data.database.dao.TransactionDao
import com.jllabs.moneylens.data.database.entities.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TransactionPagingSource(
    private val transactionDao: TransactionDao
) : PagingSource<Int, TransactionEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TransactionEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val page = params.key ?: 0
                val pageSize = params.loadSize

                val allTx = transactionDao.getAllTransactions().first()
                val fromIndex = (page * pageSize).coerceAtMost(allTx.size)
                val toIndex = ((page + 1) * pageSize).coerceAtMost(allTx.size)

                val pagedList = if (fromIndex < toIndex) {
                    allTx.subList(fromIndex, toIndex)
                } else emptyList()

                LoadResult.Page(
                    data = pagedList,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (toIndex >= allTx.size) null else page + 1
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TransactionEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
