package com.marzouki.datasaver.domain.usecase

import com.marzouki.datasaver.domain.enums.PlanStatus
import com.marzouki.datasaver.domain.model.OperatorChange
import com.marzouki.datasaver.domain.repository.Clock
import com.marzouki.datasaver.domain.repository.OperatorRepository
import com.marzouki.datasaver.domain.repository.QuotaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Configuré quand le forfait est connu (ou que l'utilisateur a dit ne pas en avoir) : le tableau
 * de bord peut alors s'afficher. Avant, ses chiffres partiraient d'un cycle au hasard (le 1er du
 * mois). Un quota déjà enregistré, d'avant cet écran, vaut configuration.
 *
 * Suivi en direct de la SIM des données : passer d'une SIM Ooredoo à une SIM Tunisie Telecom
 * redemande le forfait, car quota, solde et renouvellement ne sont plus les mêmes. Sans SIM prête
 * (mode avion, démarrage), on garde le forfait en place plutôt que de le redemander à tort.
 *
 * Le moment où la nouvelle SIM est vue pour la première fois est retenu ([OperatorChange]) : il sert
 * d'heure d'activation au forfait à saisir et déclenche une seule notification.
 */
class ObservePlanStatusUseCase @Inject constructor(
    private val repository: QuotaRepository,
    private val operatorRepository: OperatorRepository,
    private val clock: Clock = Clock { System.currentTimeMillis() },
) {
    operator fun invoke(): Flow<PlanStatus> =
        combine(
            repository.quota,
            repository.planSetupDone,
            repository.planOperatorKey,
            operatorRepository.observeDataOperator(),
            repository.operatorChange,
        ) { quota, done, planOperatorKey, operator, change ->
            when {
                quota == null && !done -> PlanStatus.MISSING
                operator == null -> PlanStatus.CONFIGURED
                operator.key == planOperatorKey -> {
                    // Forfait saisi pour cette SIM (ou ancienne SIM revenue) : plus rien en attente.
                    if (change != null) repository.setOperatorChange(null)
                    PlanStatus.CONFIGURED
                }
                planOperatorKey == null -> {
                    // Forfait saisi avant ce suivi : il est rattaché à la SIM du moment.
                    repository.setPlanOperatorKey(operator.key)
                    PlanStatus.CONFIGURED
                }
                else -> {
                    if (change?.operatorKey != operator.key) {
                        repository.setOperatorChange(OperatorChange(operator.key, operator.name, clock.nowMillis()))
                    }
                    PlanStatus.OPERATOR_CHANGED
                }
            }
        }.distinctUntilChanged()
}
