package com.plcoding.spendlesssample.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun SpendLessTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSpendLessExtraColors provides SpendLessLightExtraColors,
        LocalBodyXSmall provides BodyXSmall,
    ) {
        MaterialTheme(
            colorScheme = SpendLessLightColorScheme,
            typography = SpendLessTypography,
            content = content,
        )
    }
}
