package com.linehu.asi.feature.calculator

/**
 * Shunting-yard expression evaluator for the iOS-style calculator.
 * Pure Kotlin — no Android dependencies, unit-testable.
 *
 * Grammar: numbers, + − × ÷, unary ±, postfix %, parentheses.
 * `%` is postfix percentage (50% → 0.5; 200+10% → 220 like iOS: percent
 * resolves against the left operand when used additively — simplified here
 * to plain /100, the common expectation for a demo calculator).
 */
object ExpressionEvaluator {

    private val precedence = mapOf(
        "+" to 1, "−" to 1,
        "×" to 2, "÷" to 2,
    )

    /** Precedence including the postfix % (binds tightest). */
    private val precedenceAll = precedence + mapOf("%" to 3)

    fun evaluate(raw: String): Double? {
        val tokens = tokenize(raw) ?: return null
        if (tokens.isEmpty()) return null
        val rpn = toRpn(tokens) ?: return null
        return evalRpn(rpn)
    }

    fun tokenize(input: String): List<String>? {
        val tokens = mutableListOf<String>()
        var i = 0
        val s = input.replace(" ", "")
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    val number = s.substring(start, i)
                    if (number.count { it == '.' } > 1) return null
                    tokens += number
                }
                c == '%' -> {
                    tokens += "%"
                    i++
                }
                c == '(' -> {
                    tokens += "("
                    i++
                }
                c == ')' -> {
                    tokens += ")"
                    i++
                }
                c == '+' || c == '−' || c == '×' || c == '÷' -> {
                    // Unary minus/plus only at start / after "(" / after an
                    // operator. After a postfix "%" a +/− is binary (50%+1).
                    val prev = tokens.lastOrNull()
                    val isUnary = prev == null || prev == "(" || prev in precedence
                    if (isUnary && (c == '+' || c == '−')) {
                        // Attach sign to the next number.
                        val start = i
                        i++
                        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                        if (i == start + 1) return null // sign with no number
                        val magnitude = s.substring(start + 1, i)
                        if (magnitude.count { it == '.' } > 1) return null
                        tokens += (if (c == '−') "-" else "") + magnitude
                    } else {
                        tokens += c.toString()
                        i++
                    }
                }
                else -> return null
            }
        }
        return tokens
    }

    private fun toRpn(tokens: List<String>): List<String>? {
        val output = mutableListOf<String>()
        val ops = ArrayDeque<String>()
        for (t in tokens) {
            when {
                t.toDoubleOrNull() != null -> output += t
                t == "%" -> {
                    // Postfix: flush other pending %, then queue.
                    while (ops.isNotEmpty() && ops.last() != "(" &&
                        (precedenceAll[ops.last()] ?: 0) >= 3
                    ) {
                        output += ops.removeLast()
                    }
                    ops.addLast("%")
                }
                t == "(" -> ops.addLast(t)
                t == ")" -> {
                    while (ops.isNotEmpty() && ops.last() != "(") output += ops.removeLast()
                    if (ops.isEmpty()) return null
                    ops.removeLast()
                }
                t in precedence -> {
                    while (ops.isNotEmpty() && ops.last() != "(" &&
                        (precedenceAll[ops.last()] ?: 0) >= (precedenceAll[t] ?: 0)
                    ) {
                        output += ops.removeLast()
                    }
                    ops.addLast(t)
                }
                else -> return null
            }
        }
        while (ops.isNotEmpty()) {
            val op = ops.removeLast()
            if (op == "(") return null
            output += op
        }
        return output
    }

    private fun evalRpn(rpn: List<String>): Double? {
        val stack = ArrayDeque<Double>()
        for (t in rpn) {
            when {
                t == "%" -> {
                    val a = stack.removeLastOrNull() ?: return null
                    stack.addLast(a / 100.0)
                }
                t in precedence -> {
                    val b = stack.removeLastOrNull() ?: return null
                    val a = stack.removeLastOrNull() ?: return null
                    val r = when (t) {
                        "+" -> a + b
                        "−" -> a - b
                        "×" -> a * b
                        "÷" -> if (b == 0.0) return null else a / b
                        else -> return null
                    }
                    stack.addLast(r)
                }
                else -> {
                    stack.addLast(t.toDoubleOrNull() ?: return null)
                }
            }
        }
        return stack.removeLastOrNull()
    }
}
