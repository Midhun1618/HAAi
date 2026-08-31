package com.voxcom.haai

data class OnboardingData(
    val icon: Int,
    val title: String,
    val description: String,
    val banner: Int,
    val isDisclaimer: Boolean = false
)