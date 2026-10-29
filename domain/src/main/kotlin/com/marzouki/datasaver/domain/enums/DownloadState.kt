package com.marzouki.datasaver.domain.enums

/** Étape d'un téléchargement programmé. */
enum class DownloadState { WAITING_FOR_WIFI, RUNNING, SUCCEEDED, FAILED, CANCELLED }
