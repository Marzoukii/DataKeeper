package com.marzouki.datasaver.data.balance

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.marzouki.datasaver.domain.usecase.RecordOperatorBalanceFromTextUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lecture passive du solde : écoute les notifications affichées sur l'appareil (dont les SMS de
 * l'opérateur, qui apparaissent comme notifications) et, quand l'une annonce un solde data
 * (« il te reste 19 Go »), la mémorise pour que l'utilisateur confirme le recalage. Rien n'est
 * envoyé : c'est le message que l'opérateur a déjà posté. L'accès aux notifications est accordé par
 * l'utilisateur dans les réglages Android, et il peut le retirer à tout moment.
 */
@AndroidEntryPoint
class OperatorBalanceNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var recordBalance: RecordOperatorBalanceFromTextUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        // Le texte long (EXTRA_BIG_TEXT) porte le message complet quand il est tronqué dans EXTRA_TEXT.
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        if (body.isNullOrBlank()) return
        scope.launch { recordBalance(title, body) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
