package com.marzouki.datasaver.presentation

/** Routes des onglets, partagées avec les widgets et tuiles qui ouvrent l'appli sur un onglet précis. */
object AppRoutes {
    const val USAGE = "usage"
    const val QUOTA = "quota"
    const val DOWNLOADS = "downloads"
    const val SPEED_TEST = "speed_test"
    /** Réglages de l'appli (thème, langue), ouverts par la roue dentée de « Ma consommation ». */
    const val SETTINGS = "settings"
    /** Conso du partage de connexion, ouverte par la carte de « Ma consommation » quand il est actif. */
    const val TETHERING = "tethering"
    const val WIFI = "wifi"

    /** Extra d'Intent de [com.marzouki.datasaver.MainActivity] : route de l'onglet à ouvrir. */
    const val EXTRA_TAB = "com.marzouki.datasaver.extra.TAB"
}
