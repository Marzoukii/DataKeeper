package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.model.ForfaitSnapshot
import com.marzouki.datasaver.domain.repository.ForfaitRepository
import java.io.IOException
import javax.inject.Inject

/** Issue d'une tentative de récupération automatique du forfait. */
sealed interface ForfaitLookup {
    /** Forfait récupéré : l'écran pré-remplit ses champs avec [snapshot]. */
    data class Found(val snapshot: ForfaitSnapshot) : ForfaitLookup

    /** L'API ne couvre pas cet opérateur (ou aucune SIM) : on reste sur la saisie manuelle. */
    data object Unsupported : ForfaitLookup

    /** Réseau ou serveur injoignable : on invite à réessayer ou à saisir à la main. */
    data object Failed : ForfaitLookup
}

/**
 * Récupère le forfait auprès de l'API et le traduit en une issue nette pour l'UI, sans jamais
 * laisser remonter d'exception : une panne réseau devient [ForfaitLookup.Failed], pas un plantage.
 */
class FetchForfaitUseCase @Inject constructor(private val repository: ForfaitRepository) {
    suspend operator fun invoke(operatorKey: String?): ForfaitLookup =
        try {
            repository.fetchForfait(operatorKey)?.let(ForfaitLookup::Found) ?: ForfaitLookup.Unsupported
        } catch (_: IOException) {
            ForfaitLookup.Failed
        }
}
