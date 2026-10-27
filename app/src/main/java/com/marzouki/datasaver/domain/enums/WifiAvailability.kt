package com.marzouki.datasaver.domain.enums

/** Ce qui manque pour lister les Wi-Fi autour de soi, dans l'ordre où on le demande ; [READY] quand rien ne manque. */
enum class WifiAvailability {
    PERMISSION_MISSING,
    WIFI_OFF,
    LOCATION_OFF,
    READY,
}
