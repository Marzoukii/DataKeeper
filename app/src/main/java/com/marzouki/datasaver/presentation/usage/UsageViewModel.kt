package com.marzouki.datasaver.presentation.usage

import androidx.lifecycle.viewModelScope
import com.marzouki.datasaver.domain.usecase.GetMonthlyUsageUseCase
import com.marzouki.datasaver.domain.usecase.HasUsageAccessUseCase
import com.marzouki.datasaver.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val hasUsageAccess: HasUsageAccessUseCase,
    private val getMonthlyUsage: GetMonthlyUsageUseCase,
) : MviViewModel<UsageState, UsageIntent, UsageEffect>(UsageState()) {

    override fun onIntent(intent: UsageIntent) {
        when (intent) {
            UsageIntent.Refresh -> refresh()
            UsageIntent.GrantAccessClicked -> sendEffect(UsageEffect.OpenUsageAccessSettings)
        }
    }

    private fun refresh() {
        if (!hasUsageAccess()) {
            setState { copy(isLoading = false, hasUsageAccess = false, usage = null) }
            return
        }
        setState { copy(isLoading = true, hasUsageAccess = true) }
        viewModelScope.launch {
            getMonthlyUsage()
                .onSuccess { usage -> setState { copy(isLoading = false, usage = usage, loadFailed = false) } }
                .onFailure { setState { copy(isLoading = false, loadFailed = true) } }
        }
    }
}
