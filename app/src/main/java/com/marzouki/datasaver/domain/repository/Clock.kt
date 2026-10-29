package com.marzouki.datasaver.domain.repository

/** Heure courante, injectée : le domaine et les ViewModels ne lisent jamais l'horloge système eux-mêmes. */
fun interface Clock {
    fun nowMillis(): Long
}
