import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OptimizeCallChainTest {
    // I assume that CallChain.fromString and CallChain.toString
    // are already tested, otherwise this would be painful to test
    private fun testCallChain(expected: String, parameter: String) {
        val optimizedCallChain = optimizeCallChain(CallChain.fromString(parameter))
        assertEquals(expected, optimizedCallChain.toString())
    }

    @Test
    fun `only one map`() {
        testCallChain(
            "filter{(1=1)}%>%map{element}",
            "map{element}"
        )
    }

    @Test
    fun `only one filter`() {
        testCallChain(
            "filter{(0<element)}%>%map{element}",
            "filter{(element>0)}"
        )
    }

    @Test
    fun `filter optimization always equals`() {
        testCallChain(
            "filter{(1=1)}%>%map{element}",
            "filter{((element+3)=(element+3))}"
        )
    }

    @Test
    fun `filter optimization always not equals`() {
        testCallChain(
            "filter{(0=1)}%>%map{element}",
            "filter{((element+2)=(element+3))}"
        )
    }

    @Test
    fun `filter optimization always less`() {
        testCallChain(
            "filter{(1=1)}%>%map{element}",
            "filter{((element+2)<(element+3))}"
        )
    }

    @Test
    fun `filter optimization always not less`() {
        testCallChain(
            "filter{(0=1)}%>%map{element}",
            "filter{((element+3)<(element+3))}"
        )
    }

    @Test
    fun `filter optimization 2`() {
        testCallChain(
            "filter{(1=1)}%>%map{element}",
            "filter{(element=element)}"
        )
    }

    @Test
    fun `map optimization`() {
        testCallChain(
            "filter{(1=1)}%>%map{(((element*element)+(20*element))+100)}",
            "map{((element+10)*(element+10))}"
        )
    }

    @Test
    fun `map optimization with minus`() {
        testCallChain(
            "filter{(1=1)}%>%map{(((element*element)-(20*element))+100)}",
            "map{((element-10)*(element-10))}"
        )
    }

    @Test
    fun `filter nested expressions`() {
        testCallChain(
            "filter{((0=1)|(1=1))}%>%map{element}",
            "filter{((element>element)|(element=element))}"
        )
    }

    @Test
    fun `filter arithmetics optimization`() {
        testCallChain(
            "filter{(0=1)}%>%map{element}",
            "filter{((element+3)<(element-4))}"
        )
    }

    @Test
    fun `malformed input`() {
        val malformedCallChain = CallChain(
            listOf(
                Call(
                    CallType.FILTER,
                    Expression(OperationToken(Operation.LESS))
                )
            )
        )
        assertThrows<IllegalArgumentException> {
            optimizeCallChain(malformedCallChain)
        }
    }
}

class PolynomialTokenTest {
    @Test
    fun `PolynomialToken expressionType`() {
        assertEquals(ExpressionType.ARITHMETIC, PolynomialToken().expressionType)
    }

    private fun PolynomialToken.compareTo(vararg list: Int): Boolean {
        return polynomial == list.toList()
    }

    @Test
    fun `constructor expression NumberToken`() {
        val polynomialToken = PolynomialToken(Expression(NumberToken(3)))
        assertTrue { polynomialToken.compareTo(3) }
    }

    @Test
    fun `constructor expression ElementToken`() {
        val polynomialToken = PolynomialToken(Expression(ElementToken()))
        assertTrue { polynomialToken.compareTo(0, 1) }
    }

    @Test
    fun `constructor expression PolynomialToken`() {
        val polynomialToken = PolynomialToken(
            Expression(
                PolynomialToken(
                    listOf(
                        1, 2, 4, 3,
                    )
                )
            )
        )
        assertTrue { polynomialToken.compareTo(1, 2, 4, 3) }
    }

    @Test
    fun `constructor expression OperationToken`() {
        val polynomialToken = PolynomialToken(
            Expression(
                listOf(
                    NumberToken(3),
                    ElementToken(),
                    OperationToken(Operation.PLUS),
                )
            )
        )
        assertTrue { polynomialToken.compareTo(3, 1) }
    }

    @Test
    fun `performOperation plus`() {
        val polynomialToken1 = PolynomialToken(listOf(3))
        val polynomialToken2 = PolynomialToken(listOf(2))

        val result = polynomialToken1.performOperation(Operation.PLUS, polynomialToken2)
        assertTrue { result.compareTo(5) }
    }

    @Test
    fun `performOperation minus`() {
        val polynomialToken1 = PolynomialToken(listOf(3))
        val polynomialToken2 = PolynomialToken(listOf(2))

        val result = polynomialToken1.performOperation(Operation.MINUS, polynomialToken2)
        assertTrue { result.compareTo(1) }
    }

    @Test
    fun `performOperation multiply`() {
        val polynomialToken1 = PolynomialToken(listOf(3))
        val polynomialToken2 = PolynomialToken(listOf(2))

        val result = polynomialToken1.performOperation(Operation.MULTIPLY, polynomialToken2)
        assertTrue { result.compareTo(6) }

    }

    @Test
    fun `performOperation invalid operation`() {
        val polynomialToken1 = PolynomialToken(listOf(3))
        val polynomialToken2 = PolynomialToken(listOf(2))

        assertThrows<InvalidTypeException> {
            polynomialToken1.performOperation(Operation.LESS, polynomialToken2)
        }
    }

    @Test
    fun `plus equal size`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3, 4))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6, 1))

        val result = polynomialToken1 + polynomialToken2
        assertTrue { result.compareTo(6, 4, 9, 5) }
    }

    @Test
    fun `plus first polynomial is longer`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6, 1))

        val result = polynomialToken1 + polynomialToken2
        assertTrue { result.compareTo(6, 4, 9, 1) }
    }

    @Test
    fun `plus second polynomial is longer`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3, 4))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6))

        val result = polynomialToken1 + polynomialToken2
        assertTrue { result.compareTo(6, 4, 9, 4) }
    }

    @Test
    fun `minus equal`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3, 4))
        val polynomialToken2 = PolynomialToken(listOf(1, 2, 3, 4))

        val result = polynomialToken1 - polynomialToken2
        assertTrue { result.compareTo(0) }
    }

    @Test
    fun `minus equal size`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3, 4))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6, 1))

        val result = polynomialToken1 - polynomialToken2
        assertTrue { result.compareTo(-4, 0, -3, 3) }
    }

    @Test
    fun `minus first polynomial is longer`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6, 1))

        val result = polynomialToken1 - polynomialToken2
        assertTrue { result.compareTo(-4, 0, -3, -1) }
    }

    @Test
    fun `minus second polynomial is longer`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3, 4))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6))

        val result = polynomialToken1 - polynomialToken2
        assertTrue { result.compareTo(-4, 0, -3, 4) }
    }

    @Test
    fun `multiply equal size`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3, 4))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6, 1))

        val result = polynomialToken1 * polynomialToken2
        assertTrue { result.compareTo(5, 12, 25, 39, 28, 27, 4) }
    }

    @Test
    fun `multiply first polynomial is longer`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6, 1))

        val result = polynomialToken1 * polynomialToken2
        assertTrue { result.compareTo(5, 12, 25, 19, 20, 3) }
    }

    @Test
    fun `multiply second polynomial is longer`() {
        val polynomialToken1 = PolynomialToken(listOf(1, 2, 3, 4))
        val polynomialToken2 = PolynomialToken(listOf(5, 2, 6))

        val result = polynomialToken1 * polynomialToken2
        assertTrue { result.compareTo(5, 12, 25, 38, 26, 24) }
    }

    @Test
    fun `toExpression empty`() {
        val expression = PolynomialToken().toExpression()

        assertEquals(1, expression.tokens.size)
        assertEquals(NumberToken::class, expression.tokens.first()::class)
        assertEquals(0, (expression.tokens.first() as NumberToken).number)
    }

    @Test
    fun `toExpression one`() {
        val expression = PolynomialToken(listOf(1)).toExpression()

        assertEquals(1, expression.tokens.size)
        assertEquals(NumberToken::class, expression.tokens.first()::class)
        assertEquals(1, (expression.tokens.first() as NumberToken).number)
    }

    @Test
    fun `toExpression x^2+1`() {
        val actual = PolynomialToken(listOf(1, 0, 1)).toExpression()
        val expected = Expression(
            listOf(
                ElementToken(),
                ElementToken(),
                OperationToken(Operation.MULTIPLY),
                NumberToken(1),
                OperationToken(Operation.PLUS),
            )
        )
        assertEquals(expected.tokens.size, actual.tokens.size, "expression sizes does not match")
        for (i in expected.tokens.indices) {
            assertEquals(expected.tokens[i], actual.tokens[i])
        }
    }

    @Test
    fun `toString empty`() {
        assertEquals("0", PolynomialToken().toString())
    }

    @Test
    fun `toString not empty`() {
        assertEquals("((2*element)+1)", PolynomialToken(listOf(1, 2)).toString())
    }
}