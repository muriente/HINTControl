package dev.zwander.common.util

import dev.zwander.common.model.adapters.WifiConfig

internal data class WifiSaveResult(
    val accepted: Boolean,
    val observed: WifiConfig?,
    val unconfirmedRadios: List<String> = emptyList(),
) {
    val verified: Boolean get() = accepted && observed != null && unconfirmedRadios.isEmpty()
}

internal suspend fun saveWifiAndReadBack(
    requested: WifiConfig,
    write: suspend (WifiConfig) -> Boolean,
    read: suspend () -> WifiConfig?,
): WifiSaveResult {
    if (!write(requested)) return WifiSaveResult(false, null)
    val observed = read() ?: return WifiSaveResult(true, null)
    val unconfirmed = listOf(
        Triple("2.4 GHz", requested.twoGig, observed.twoGig),
        Triple("5 GHz", requested.fiveGig, observed.fiveGig),
        Triple("6 GHz", requested.sixGig, observed.sixGig),
    ).mapNotNull { (name, expected, actual) ->
        name.takeIf {
            expected?.isRadioEnabled != null && expected.isRadioEnabled != actual?.isRadioEnabled
        }
    }
    return WifiSaveResult(true, observed, unconfirmed)
}
