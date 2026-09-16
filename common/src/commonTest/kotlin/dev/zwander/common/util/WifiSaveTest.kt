package dev.zwander.common.util

import dev.zwander.common.model.adapters.BandConfig
import dev.zwander.common.model.adapters.WifiConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WifiSaveTest {
    private val requested = WifiConfig(
        twoGig = BandConfig(isRadioEnabled = false),
        fiveGig = BandConfig(isRadioEnabled = false),
    )

    @Test
    fun radioOffIsIncludedInRequest() {
        val json = Json.encodeToString(requested)
        assertTrue(json.contains("\"isRadioEnabled\":false"))
        assertEquals(requested, Json.decodeFromString<WifiConfig>(json))
    }

    @Test
    fun acceptedRequestWithIgnoredRadioSettingsIsNotVerified() = runTest {
        val actual = requested.copy(twoGig = BandConfig(isRadioEnabled = true))
        val result = saveWifiAndReadBack(requested, { true }, { actual })
        assertEquals(actual, result.observed)
        assertEquals(listOf("2.4 GHz"), result.unconfirmedRadios)
        assertFalse(result.verified)
    }

    @Test
    fun matchingReadbackConfirmsRadioSettings() = runTest {
        val result = saveWifiAndReadBack(requested, { true }, { requested })
        assertTrue(result.verified)
        assertEquals(requested, result.observed)
    }

    @Test
    fun failedWriteDoesNotReadOrReplaceDraft() = runTest {
        val result = saveWifiAndReadBack(requested, { false }, { error("Must not read after a failed write") })
        assertFalse(result.accepted)
        assertNull(result.observed)
        assertFalse(result.verified)
    }

    @Test
    fun unavailableReadbackDoesNotConfirmShutdown() = runTest {
        val result = saveWifiAndReadBack(requested, { true }, { null })
        assertTrue(result.accepted)
        assertFalse(result.verified)
        assertNull(result.observed)
    }

    @Test
    fun missingRadioStateIsUnconfirmedRatherThanOff() = runTest {
        val result = saveWifiAndReadBack(requested, { true }, { WifiConfig() })
        assertEquals(listOf("2.4 GHz", "5 GHz"), result.unconfirmedRadios)
        assertFalse(result.verified)
    }

    @Test
    fun sixGhzRadioIsAlsoVerifiedWhenPresent() = runTest {
        val withSix = requested.copy(sixGig = BandConfig(isRadioEnabled = false))
        val result = saveWifiAndReadBack(withSix, { true }, { requested })
        assertEquals(listOf("6 GHz"), result.unconfirmedRadios)
    }
}
