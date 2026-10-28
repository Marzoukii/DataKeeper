package com.marzouki.datasaver.data.analytics

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.marzouki.datasaver.domain.repository.CrashReporter
import javax.inject.Inject

/** Erreurs non fatales envoyées à Firebase Crashlytics, à côté des plantages qu'il relève seul. */
class FirebaseCrashReporter @Inject constructor() : CrashReporter {

    override fun recordException(throwable: Throwable) {
        Firebase.crashlytics.recordException(throwable)
    }
}
