package com.linehu.asi.feature.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpressionEvaluatorTest {

    @Test
    fun `simple addition`() {
        assertEquals(5.0, ExpressionEvaluator.evaluate("2+3")!!, 1e-9)
    }

    @Test
    fun `operator precedence - multiplication before addition`() {
        // 2 + 3 × 4 = 14, not 20
        assertEquals(14.0, ExpressionEvaluator.evaluate("2+3×4")!!, 1e-9)
    }

    @Test
    fun `division and subtraction chain`() {
        // 10 − 2 = 8 (÷ binds tighter than −)
        assertEquals(8.0, ExpressionEvaluator.evaluate("10−4÷2")!!, 1e-9)
    }

    @Test
    fun `parentheses override precedence`() {
        assertEquals(18.0, ExpressionEvaluator.evaluate("(2+4)×3")!!, 1e-9)
    }

    @Test
    fun `postfix percentage`() {
        assertEquals(0.5, ExpressionEvaluator.evaluate("50%")!!, 1e-9)
    }

    @Test
    fun `unary minus`() {
        assertEquals(-7.0, ExpressionEvaluator.evaluate("−3−4")!!, 1e-9)
        assertEquals(-3.0, ExpressionEvaluator.evaluate("−3")!!, 1e-9)
    }

    @Test
    fun `unary minus after open paren`() {
        assertEquals(-6.0, ExpressionEvaluator.evaluate("2×(−3)")!!, 1e-9)
    }

    @Test
    fun `decimal numbers`() {
        assertEquals(1.5, ExpressionEvaluator.evaluate("0.75+0.75")!!, 1e-9)
    }

    @Test
    fun `division by zero yields null`() {
        assertNull(ExpressionEvaluator.evaluate("5÷0"))
    }

    @Test
    fun `dangling operator yields null`() {
        assertNull(ExpressionEvaluator.evaluate("2+"))
        assertNull(ExpressionEvaluator.evaluate("×3"))
    }

    @Test
    fun `unbalanced parens yield null`() {
        assertNull(ExpressionEvaluator.evaluate("(2+3"))
        assertNull(ExpressionEvaluator.evaluate("2+3)"))
    }

    @Test
    fun `empty expression yields null`() {
        assertNull(ExpressionEvaluator.evaluate(""))
    }

    @Test
    fun `chained percent and paren`() {
        assertEquals(150.0, ExpressionEvaluator.evaluate("(50%+1)×100")!!, 1e-9)
    }
}
