package com.marzouki.datasaver.data.analytics

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.marzouki.datasaver.domain.repository.AnalyticsTracker
import javax.inject.Inject

/**
 * Firebase Analytics ne voit qu'une activité : les écrans Compose sont signalés à la main,
 * pour que les rapports distinguent les onglets.
 */
class FirebaseAnalyticsTracker @Inject constructor() : AnalyticsTracker {

    override fun logScreenView(screenName: String) {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }
}
