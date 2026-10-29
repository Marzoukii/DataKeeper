package com.marzouki.datasaver.domain.enums

/** Génération du réseau mobile de la SIM des données, telle qu'affichée dans la barre d'état. */
enum class NetworkGeneration(val label: String) {
    G2("2G"),
    G3("3G"),
    G4("4G"),
    G5("5G"),
}
