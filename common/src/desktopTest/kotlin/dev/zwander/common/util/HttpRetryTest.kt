package dev.zwander.common.util

import dev.zwander.common.model.GlobalModel
import dev.zwander.common.model.adapters.CellDataRoot
import dev.zwander.common.model.adapters.ClientDeviceData
import dev.zwander.common.model.adapters.MainData
import dev.zwander.common.model.adapters.SimDataRoot
import dev.zwander.common.model.adapters.WifiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HttpRetryTest {
    @Test
    fun successfulRetryReturnsResponseToOriginalCaller() = runTest {
        var requests = 0
        val client = HttpClient(MockEngine {
            requests++
            if (requests == 1) {
                respond("retry", HttpStatusCode.RequestTimeout)
            } else {
                respond("retained response", HttpStatusCode.OK)
            }
        })
        try {
            val api = TestClient(client)
            val response = with(api) {
                client.handleCatch(
                    showError = false,
                    reportError = false,
                    retryForLive = true,
                    maxRetries = 2,
                ) {
                    get(testUrl)
                }
            }

            assertEquals(2, requests)
            assertEquals(HttpStatusCode.OK, assertNotNull(response).status)
            assertEquals("retained response", response.bodyAsText())
        } finally {
            client.close()
        }
    }

    @Test
    fun cancellationPropagatesWithoutRetrying() = runTest {
        val cancellation = CancellationException("Request cancelled")
        var attempts = 0
        val client = HttpClient(MockEngine { error("No request should reach the engine") })
        try {
            val api = TestClient(client)
            val thrown = assertFailsWith<CancellationException> {
                with(api) {
                    client.handleCatch(
                        showError = false,
                        reportError = false,
                        retryForLive = true,
                    ) {
                        attempts++
                        throw cancellation
                    }
                }
            }

            assertSame(cancellation, thrown)
            assertEquals(1, attempts)
        } finally {
            client.close()
        }
    }

    @Test
    fun loaderPropagatesCancellationAndClearsLoadingState() = runTest {
        val client = HttpClient(MockEngine { error("No request should reach the engine") })
        val previousBlocking = GlobalModel.isBlocking.value
        val previousLoading = GlobalModel.isLoading.value
        val previousError = GlobalModel.httpError.value
        try {
            val api = TestClient(client)
            for (blocking in listOf(false, true)) {
                GlobalModel.isBlocking.value = false
                GlobalModel.isLoading.value = false
                val cancellation = CancellationException("Save cancelled")

                val thrown = assertFailsWith<CancellationException> {
                    api.withLoader(blocking) {
                        assertTrue(if (blocking) GlobalModel.isBlocking.value else GlobalModel.isLoading.value)
                        throw cancellation
                    }
                }

                assertSame(cancellation, thrown)
                assertFalse(GlobalModel.isBlocking.value)
                assertFalse(GlobalModel.isLoading.value)
                assertSame(previousError, GlobalModel.httpError.value)
            }
        } finally {
            GlobalModel.isBlocking.value = previousBlocking
            GlobalModel.isLoading.value = previousLoading
            client.close()
        }
    }

    private class TestClient(override val httpClient: HttpClient) : HTTPClient {
        override val testUrl = "https://gateway.invalid/"
        override val unauthedClient = httpClient
        override val isUnifiedApi = true

        override suspend fun logIn(username: String, password: String, rememberCredentials: Boolean): Unit = error("Unused")
        override suspend fun getMainData(unauthed: Boolean): MainData? = error("Unused")
        override suspend fun getWifiData(): WifiConfig? = error("Unused")
        override suspend fun getDeviceData(): ClientDeviceData? = error("Unused")
        override suspend fun getCellData(): CellDataRoot? = error("Unused")
        override suspend fun getSimData(): SimDataRoot? = error("Unused")
        override suspend fun setWifiData(newData: WifiConfig): Boolean = error("Unused")
        override suspend fun setLogin(newUsername: String, newPassword: String): Unit = error("Unused")
        override suspend fun reboot(): Unit = error("Unused")
        override suspend fun exists(): Boolean = error("Unused")
    }
}
