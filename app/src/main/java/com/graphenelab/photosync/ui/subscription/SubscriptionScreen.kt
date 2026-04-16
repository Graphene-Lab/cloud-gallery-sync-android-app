package com.graphenelab.photosync.ui.subscription

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.graphenelab.photosync.data.network.payment.SubscriptionPlan
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val plans by viewModel.plans.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val paymentSheet = rememberPaymentSheet { paymentSheetResult ->
        onPaymentSheetResult(paymentSheetResult, context)
    }


    val stripePublishableKey = com.graphenelab.photosync.BuildConfig.STRIPE_PUBLIC_KEY
    LaunchedEffect(Unit) {
        if (stripePublishableKey.startsWith("pk_test_")) {
            PaymentConfiguration.init(context, stripePublishableKey)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Subscription Plans") })
        }
    ) { paddingValues ->
        if (plans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(plans) { plan ->
                    PlanItem(plan) {
                        coroutineScope.launch {
                            try {
                                val clientSecret = viewModel.createPaymentIntent(plan.name)
                                paymentSheet.presentWithPaymentIntent(clientSecret)
                            } catch (e: Exception) {
                                // Handle error creating payment intent
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanItem(plan: SubscriptionPlan, onSubscribeClicked: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(plan.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Price: ${plan.displayAmount}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSubscribeClicked,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Subscribe")
            }
        }
    }
}

private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult, context: android.content.Context) {
    when (paymentSheetResult) {
        is PaymentSheetResult.Completed -> {
            Toast.makeText(context, "Payment successful!", Toast.LENGTH_LONG).show()
        }
        is PaymentSheetResult.Canceled -> {
            Toast.makeText(context, "Payment canceled.", Toast.LENGTH_LONG).show()
        }
        is PaymentSheetResult.Failed -> {
            Toast.makeText(context, "Payment failed: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
        }
    }
}