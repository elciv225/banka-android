package com.banka.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.banka.app.domain.CategoryTotal
import com.banka.app.domain.MonthlyTotals
import com.banka.app.util.Formatters

@Composable
fun MonthlyBarsChart(months: List<MonthlyTotals>, modifier: Modifier = Modifier) {
    val progress = remember(months.map { it.month }) { Animatable(0f) }
    LaunchedEffect(months.map { it.month }) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val max = months.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0.0 } ?: 1.0

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(bottom = 6.dp),
        ) {
            if (months.isEmpty()) return@Canvas
            val slot = size.width / months.size
            val barWidth = slot * 0.26f
            val gap = slot * 0.06f
            months.forEachIndexed { index, month ->
                val centerX = slot * index + slot / 2f
                val incomeHeight = ((month.income / max).toFloat() * size.height * progress.value).toFloat()
                val expenseHeight = ((month.expense / max).toFloat() * size.height * progress.value).toFloat()
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(centerX - barWidth - gap, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
                )
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(centerX + gap, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(barWidth / 3f, barWidth / 3f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            months.forEach { month ->
                Text(
                    text = Formatters.monthShortLabel(month.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun DonutChart(slices: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices.size, slices.firstOrNull()?.category?.key) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 40f, cap = StrokeCap.Butt)
        var start = -90f
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        slices.forEach { slice ->
            val sweep = slice.share * 360f * progress.value
            drawArc(
                color = Color(slice.category.color),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke,
            )
            start += slice.share * 360f
        }
    }
}

@Composable
fun CategoryLegend(slices: List<CategoryTotal>, modifier: Modifier = Modifier, max: Int = 6) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.take(max).forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(slice.category.color)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${slice.category.emoji} ${slice.category.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Formatters.amount(slice.total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
