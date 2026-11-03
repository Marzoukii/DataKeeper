package com.marzouki.datasaver.domain

import com.marzouki.datasaver.domain.enums.TunisianOperator
import com.marzouki.datasaver.domain.model.OperatorBalanceSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OperatorBalanceSmsTest {

    @Test
    fun `expediteur reconnu par son identifiant`() {
        assertEquals(TunisianOperator.OOREDOO, OperatorBalanceSmsParser.operatorOf("Ooredoo"))
        assertEquals(TunisianOperator.ORANGE, OperatorBalanceSmsParser.operatorOf("Orange"))
        assertEquals(TunisianOperator.TUNISIE_TELECOM, OperatorBalanceSmsParser.operatorOf("TT"))
        assertEquals(TunisianOperator.TUNISIE_TELECOM, OperatorBalanceSmsParser.operatorOf("Tunisie Telecom"))
        assertEquals(TunisianOperator.TUNISIE_TELECOM, OperatorBalanceSmsParser.operatorOf("Télécom"))
        assertNull(OperatorBalanceSmsParser.operatorOf("12345"))
        assertNull(OperatorBalanceSmsParser.operatorOf(null))
    }

    @Test
    fun `solde Ooredoo avec decimale`() {
        val sms = OperatorBalanceSmsParser.parse("Ooredoo", "Cher client, il vous reste 19,3 Go valable jusqu'au 01/12.")
        assertEquals(TunisianOperator.OOREDOO, sms?.operator)
        assertEquals(19_300_000_000L, sms?.remainingBytes)
    }

    @Test
    fun `prend le reste et non le total dans 19 Go sur 25 Go`() {
        val sms = OperatorBalanceSmsParser.parse("Ooredoo", "Il vous reste 19 Go sur 25 Go.")
        assertEquals(19_000_000_000L, sms?.remainingBytes)
    }

    @Test
    fun `solde en Mo chez Tunisie Telecom`() {
        val sms = OperatorBalanceSmsParser.parse("TT", "Votre solde internet est de 2500 Mo.")
        assertEquals(TunisianOperator.TUNISIE_TELECOM, sms?.operator)
        assertEquals(2_500_000_000L, sms?.remainingBytes)
    }

    @Test
    fun `message en arabe avec reste`() {
        val sms = OperatorBalanceSmsParser.parse("Orange", "عزيزي الحريف، رصيدك المتبقي 5 Go")
        assertEquals(TunisianOperator.ORANGE, sms?.operator)
        assertEquals(5_000_000_000L, sms?.remainingBytes)
    }

    @Test
    fun `expediteur inconnu avec mot de reste accepte`() {
        val sms = OperatorBalanceSmsParser.parse("Info", "Il te reste 3 Go ce mois-ci.")
        assertNull(sms?.operator)
        assertEquals(3_000_000_000L, sms?.remainingBytes)
    }

    @Test
    fun `sms promotionnel sans mot de reste rejete`() {
        val sms = OperatorBalanceSmsParser.parse("Promo", "Rechargez et gagnez 10 Go gratuits !")
        assertNull(sms)
    }

    @Test
    fun `sms sans volume data rejete`() {
        assertNull(OperatorBalanceSmsParser.parse("Ooredoo", "Merci de votre fidélité."))
        assertNull(OperatorBalanceSmsParser.parse("Ooredoo", ""))
    }

    @Test
    fun `petites unites`() {
        assertEquals(100_000L, OperatorBalanceSmsParser.parse("Ooredoo", "Il reste 100 Ko.")?.remainingBytes)
    }
}
