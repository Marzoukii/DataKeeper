package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.model.ForfaitSnapshot
import com.marzouki.datasaver.domain.repository.ForfaitRepository
import com.marzouki.datasaver.domain.usecase.FetchForfaitUseCase
import com.marzouki.datasaver.domain.usecase.ForfaitLookup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

private const val GB = 1_000_000_000L

class FetchForfaitUseCaseTest {

    private class FakeRepo(val snapshot: ForfaitSnapshot? = null, val error: Throwable? = null) : ForfaitRepository {
        override suspend fun fetchForfait(operatorKey: String?): ForfaitSnapshot? {
            error?.let { throw it }
            return snapshot
        }
    }

    @Test
    fun `un forfait renvoye donne Found`() = runTest {
        val snapshot = ForfaitSnapshot(25 * GB, 19 * GB, 30, 28_500)
        val result = FetchForfaitUseCase(FakeRepo(snapshot = snapshot))("60503")
        assertEquals(ForfaitLookup.Found(snapshot), result)
    }

    @Test
    fun `l'absence de forfait donne Unsupported`() = runTest {
        val result = FetchForfaitUseCase(FakeRepo(snapshot = null))("00000")
        assertEquals(ForfaitLookup.Unsupported, result)
    }

    @Test
    fun `une panne reseau donne Failed sans propager l'exception`() = runTest {
        val result = FetchForfaitUseCase(FakeRepo(error = IOException("serveur KO")))("60503")
        assertEquals(ForfaitLookup.Failed, result)
    }
}
