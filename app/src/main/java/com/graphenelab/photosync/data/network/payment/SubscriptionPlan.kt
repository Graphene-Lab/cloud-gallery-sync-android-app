package com.graphenelab.photosync.data.network.payment

/**
 * Data class representing a subscription plan as received from the backend API.
 */
data class SubscriptionPlan(
    val id: Long,
    val name: String, // TODO: make into Enum
    val amount: Long, // Represents the amount in cents (or smallest currency unit)
    val description: String,
    val displayAmount: String, // Already formatted for display (e.g. 9.50$)
    val currency: String // The currency code (e.g., "USD", "EUR")
)