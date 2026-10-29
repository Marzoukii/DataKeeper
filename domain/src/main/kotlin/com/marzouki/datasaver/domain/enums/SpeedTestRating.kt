package com.marzouki.datasaver.domain.enums

/** Appréciation du débit descendant, selon les usages qu'il permet. */
enum class SpeedTestRating(val minDownloadMbps: Double) {
    EXCELLENT(25.0),
    GOOD(10.0),
    FAIR(3.0),
    SLOW(0.0),
    ;

    companion object {
        fun from(downloadMbps: Double): SpeedTestRating = entries.first { downloadMbps >= it.minDownloadMbps }
    }
}
