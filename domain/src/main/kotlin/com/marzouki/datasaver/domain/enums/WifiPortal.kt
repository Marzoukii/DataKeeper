package com.marzouki.datasaver.domain.enums

/**
 * Ce qu'on sait d'un Wi-Fi ouvert après s'y être connecté : [WITH_PORTAL] quand Android y a détecté
 * un portail de connexion (page à valider, souvent avec pub), [NO_PORTAL] quand internet passait
 * directement. [UNKNOWN] tant qu'on ne s'y est jamais connecté avec l'appli.
 */
enum class WifiPortal {
    UNKNOWN,
    WITH_PORTAL,
    NO_PORTAL,
}
