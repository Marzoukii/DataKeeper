package com.marzouki.datasaver.presentation

/** Routes des onglets, partagées avec les widgets et tuiles qui ouvrent l'appli sur un onglet précis. */
object AppRoutes {
    const val USAGE = "usage"
    const val QUOTA = "quota"
    const val DOWNLOADS = "downloads"

    /** Extra d'Intent de [com.marzouki.datasaver.MainActivity] : route de l'onglet à ouvrir. */
    const val EXTRA_TAB = "com.marzouki.datasaver.extra.TAB"
}
