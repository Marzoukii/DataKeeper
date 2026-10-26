package com.marzouki.datasaver.presentation

import com.marzouki.datasaver.presentation.onboarding.PlanSetupState
import com.marzouki.datasaver.presentation.quota.PlanCycleInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanSetupFormTest {

    @Test
    fun `taille sur une puce, pas de champ libre`() {
        assertFalse(PlanSetupState(planInput = "25").showsCustomPlan)
        assertFalse(PlanSetupState().showsCustomPlan)
    }

    @Test
    fun `taille hors des puces ou Autre choisi, champ libre`() {
        assertTrue(PlanSetupState(planInput = "30").showsCustomPlan)
        assertTrue(PlanSetupState(planInput = "2,5").showsCustomPlan)
        // « Autre » puis 25 tapé : le champ reste, la puce 25 Go ne s'allume pas.
        assertTrue(PlanSetupState(planInput = "25", customPlan = true).showsCustomPlan)
    }

    @Test
    fun `30 jours par défaut, sur une puce`() {
        val cycle = PlanCycleInput()
        assertEquals(30, cycle.validityDays)
        assertFalse(cycle.showsCustomValidity)
    }

    @Test
    fun `pass de 4 jours déjà enregistré, champ en jours`() {
        assertTrue(PlanCycleInput(validityInput = "4").showsCustomValidity)
    }

    @Test
    fun `Autre choisi ou abonnement mensuel`() {
        assertTrue(PlanCycleInput(validityInput = "15", customValidity = true).showsCustomValidity)
        assertFalse(PlanCycleInput(monthly = true).showsCustomValidity)
    }
}
