package com.plcoding.spendlesssample.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.plcoding.spendlesssample.core.designsystem.R

/**
 * Icons from the Figma design-system page, regenerated with the 24×24 frame as the viewport
 * (research R5). Each drawable links its Figma node. Tint at the call site.
 */
object SpendLessIcons {
    val ArrowBack: Painter @Composable get() = painterResource(R.drawable.ic_arrow_back)
    val Close: Painter @Composable get() = painterResource(R.drawable.ic_close)
    val Add: Painter @Composable get() = painterResource(R.drawable.ic_add)
    val Check: Painter @Composable get() = painterResource(R.drawable.ic_check)
    val ArrowDropDown: Painter @Composable get() = painterResource(R.drawable.ic_arrow_drop_down)
    val Notes: Painter @Composable get() = painterResource(R.drawable.ic_notes)
    val TrendingDown: Painter @Composable get() = painterResource(R.drawable.ic_trending_down)
    val TrendingUp: Painter @Composable get() = painterResource(R.drawable.ic_trending_up)
}
