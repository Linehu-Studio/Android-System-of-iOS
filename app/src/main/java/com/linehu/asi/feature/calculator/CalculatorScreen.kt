package com.linehu.asi.feature.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.IosText

private val DisplayBg = Color(0xFF000000)
private val BtnGray = Color(0xFF333336)
private val BtnLight = Color(0xFFA5A5A5)
private val BtnOrange = Color(0xFFFF9F0A)

/** iOS-style calculator: dark, round buttons, orange operators. */
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var lastAddedWasOp by remember { mutableStateOf(false) }

    fun append(token: String) {
        val isOperator = token in listOf("+", "−", "×", "÷")
        when {
            isOperator -> {
                if (expression.isEmpty()) return
                if (lastAddedWasOp) {
                    expression = expression.dropLast(1) + token
                } else {
                    expression += token
                }
                lastAddedWasOp = true
            }
            token == "." -> {
                val lastNumber = expression.split('+', '−', '×', '÷', '(', ')').last()
                if ('.' in lastNumber) return
                expression += token
                lastAddedWasOp = false
            }
            token == "(" -> {
                expression += token
                lastAddedWasOp = false
            }
            token == ")" -> {
                expression += token
                lastAddedWasOp = false
            }
            else -> {
                expression += token
                lastAddedWasOp = false
            }
        }
        result = null
    }

    fun clear() {
        expression = ""
        result = null
        lastAddedWasOp = false
    }

    fun equals() {
        val value = ExpressionEvaluator.evaluate(expression)
        result = value?.let { formatNumber(it) } ?: "错误"
    }

    /** Negate the trailing number (iOS ± behavior). */
    fun toggleSign() {
        if (expression.isEmpty()) return
        val match = Regex("(-?\\d*\\.?\\d+)$").find(expression) ?: return
        val number = match.value
        expression = when {
            number.startsWith("-") -> expression.dropLast(number.length) + number.drop(1)
            else -> expression.dropLast(number.length) + "-" + number
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(DisplayBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        IosStatusBar(contentColor = Color.White)
        // Display
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
        ) {
            IosText(
                text = result ?: expression.ifEmpty { "0" },
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
        // Keypad
        val rows = listOf(
            listOf("AC" to BtnLight, "±" to BtnLight, "%" to BtnLight, "÷" to BtnOrange),
            listOf("7" to BtnGray, "8" to BtnGray, "9" to BtnGray, "×" to BtnOrange),
            listOf("4" to BtnGray, "5" to BtnGray, "6" to BtnGray, "−" to BtnOrange),
            listOf("1" to BtnGray, "2" to BtnGray, "3" to BtnGray, "+" to BtnOrange),
            listOf("0" to BtnGray, "." to BtnGray, "=" to BtnOrange),
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().height(72.dp)) {
                    row.forEach { (label, color) ->
                        val spanTwo = label == "0"
                        Box(
                            Modifier
                                .weight(if (spanTwo) 2f else 1f)
                                .height(66.dp)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    when (label) {
                                        "AC" -> clear()
                                        "±" -> toggleSign()
                                        "=" -> equals()
                                        else -> append(label)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            IosText(
                                text = label,
                                fontSize = 32.sp,
                                color = if (color == BtnLight) Color.Black else Color.White,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun formatNumber(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "错误"
    return if (v == v.toLong().toDouble() && kotlin.math.abs(v) < 1e12) {
        v.toLong().toString()
    } else {
        val s = v.toString()
        if (s.length > 12) String.format("%.6g", v) else s
    }
}
