package com.cloud.sync.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloud.sync.data.network.payment.SubscriptionPlan
import com.cloud.sync.domain.repositroy.IPaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val paymentRepository: IPaymentRepository
) : ViewModel() {

    private val _plans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val plans = _plans.asStateFlow()

    init {
        loadSubscriptionPlans()
    }

    private fun loadSubscriptionPlans() {
        viewModelScope.launch {
            _plans.value = paymentRepository.getSubscriptionPlans()
        }
    }

    suspend fun createPaymentIntent(planId: String): String {
        return paymentRepository.createPaymentIntent(planId)
    }
}