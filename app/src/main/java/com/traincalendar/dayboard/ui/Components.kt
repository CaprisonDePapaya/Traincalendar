package com.traincalendar.dayboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traincalendar.dayboard.data.Line
import com.traincalendar.dayboard.data.LineShape
import com.traincalendar.dayboard.data.badgeTextIsDark

object Board {
    val Black = Color(0xFF0B0B0B)
    val Panel = Color(0xFF1A1A1A)
    val PanelLine = Color(0xFF2C2C2C)
    val Cream = Color(0xFFF5F2EA)
    val LiveCream = Color(0xFFEFE9DA)
    val Divider = Color(0xFFDDD7C9)
    val Yellow = Color(0xFFFCCC0A)
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF6E6A62)
    val Dim = Color(0xFF8A8A8A)
    val Red = Color(0xFFEE352E)
}

@Composable
fun DayBoardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Board.Yellow,
            onPrimary = Board.Ink,
            background = Board.Black,
            surface = Board.Panel,
            onSurface = Color.White,
            surfaceVariant = Board.PanelLine,
        ),
        content = content,
    )
}

/** A subway route bullet: a coloured circle (or diamond) with the line letter. */
@Composable
fun LineBadge(line: Line, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    val fill = Color(line.color)
    val textColor = if (badgeTextIsDark(line.color)) Board.Ink else Color.White
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (line.shape == LineShape.DIAMOND) {
            Box(
                Modifier
                    .size(size * 0.72f)
                    .rotate(45f)
                    .background(fill, RoundedCornerShape(size * 0.08f))
            )
        } else {
            Box(Modifier.size(size).background(fill, CircleShape))
        }
        Text(
            line.letter.take(2),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.46f).sp,
        )
    }
}

/** The hollow bullet used on the yellow "live" row. */
@Composable
fun OutlinedLineBadge(line: Line, size: Dp = 32.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawCircle(Board.Ink, radius = this.size.minDimension / 2 - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        }
        Text(line.letter.take(2), color = Board.Ink, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.46f).sp)
    }
}

enum class Tab(val label: String) { BOARD("BOARD"), LINES("LINES"), ADD("ADD") }

/** Small line-art icons for the bottom bar, drawn to match the signage style. */
@Composable
fun TabIcon(tab: Tab, color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val s = size.minDimension
        when (tab) {
            Tab.BOARD -> {
                val g = s * 0.12f
                val c = (s - g) / 2
                for (x in 0..1) for (y in 0..1) {
                    drawRoundRect(
                        color,
                        topLeft = Offset(x * (c + g), y * (c + g)),
                        size = Size(c, c),
                        cornerRadius = CornerRadius(s * 0.06f),
                    )
                }
            }
            Tab.LINES -> {
                val stroke = s * 0.09f
                val a = Offset(s * 0.2f, s * 0.2f)
                val b = Offset(s * 0.8f, s * 0.2f)
                val m = Offset(s * 0.5f, s * 0.55f)
                val d = Offset(s * 0.5f, s * 0.88f)
                drawLine(color, a, m, stroke, StrokeCap.Round)
                drawLine(color, b, m, stroke, StrokeCap.Round)
                drawLine(color, m, d, stroke, StrokeCap.Round)
                listOf(a, b, m, d).forEach {
                    drawCircle(Board.Black, s * 0.11f, it)
                    drawCircle(color, s * 0.11f, it, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.8f))
                }
            }
            Tab.ADD -> {
                val stroke = s * 0.11f
                drawLine(color, Offset(s / 2, s * 0.1f), Offset(s / 2, s * 0.9f), stroke, StrokeCap.Round)
                drawLine(color, Offset(s * 0.1f, s / 2), Offset(s * 0.9f, s / 2), stroke, StrokeCap.Round)
            }
        }
    }
}

/** A small outlined trash can, drawn to match the tab icons. */
@Composable
fun TrashIcon(color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val s = size.minDimension
        val stroke = s * 0.09f
        drawLine(color, Offset(s * 0.12f, s * 0.24f), Offset(s * 0.88f, s * 0.24f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.38f, s * 0.1f), Offset(s * 0.62f, s * 0.1f), stroke, StrokeCap.Round)
        drawRoundRect(
            color,
            topLeft = Offset(s * 0.22f, s * 0.24f),
            size = Size(s * 0.56f, s * 0.68f),
            cornerRadius = CornerRadius(s * 0.08f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawLine(color, Offset(s * 0.42f, s * 0.42f), Offset(s * 0.42f, s * 0.76f), stroke, StrokeCap.Round)
        drawLine(color, Offset(s * 0.58f, s * 0.42f), Offset(s * 0.58f, s * 0.76f), stroke, StrokeCap.Round)
    }
}
