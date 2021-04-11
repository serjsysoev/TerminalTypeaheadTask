import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CallChainTest {
    @Test
    fun `toString empty`() {
        assertEquals("", CallChain(emptyList()).toString())
    }

    @Test
    fun `toString one call`() {
        val callChain = CallChain(
            listOf(
                Call(CallType.MAP, Expression(NumberToken(3)))
            )
        )
        assertEquals("map{3}", callChain.toString())
    }

    @Test
    fun `toString two calls`() {
        val call = Call(CallType.MAP, Expression(ElementToken()))
        val callChain = CallChain(listOf(call, call))
        assertEquals("map{element}%>%map{element}", callChain.toString())
    }

    private val callString = "map{element}"
    private fun verifyCall(call: Call) {
        assertEquals(CallType.MAP, call.type, "Incorrect call type")

        val expression = call.expression
        assertEquals(1, expression.tokens.size, "Incorrect number of tokens")
        assertEquals(
            ElementToken::class, expression.tokens.first()::class,
            "Expression token classes do not match"
        )
    }

    @Test
    fun `fromString empty string`() {
        assertThrows<InvalidSyntaxException> {
            CallChain.fromString("")
        }
    }

    @Test
    fun `fromString one call`() {
        val callChain = CallChain.fromString(callString)
        assertEquals(1, callChain.calls.size, "Incorrect number of calls")
        verifyCall(callChain.calls.first())
    }

    @Test
    fun `fromString two calls`() {
        val callChain = CallChain.fromString("$callString%>%$callString")
        assertEquals(2, callChain.calls.size, "Incorrect number of calls")
        verifyCall(callChain.calls[0])
        verifyCall(callChain.calls[1])
    }
}

class CallTest {
    @Test
    fun `init mismatched types`() {
        assertThrows<InvalidTypeException> {
            Call(CallType.FILTER, Expression(ElementToken()))
        }
    }

    @Test
    fun `toString map`() {
        val call = Call(CallType.MAP, Expression(ElementToken()))
        assertEquals("map{element}", call.toString())
    }

    @Test
    fun `fromString missing braces`() {
        assertThrows<InvalidSyntaxException>("Right brace check failed") {
            Expression.fromString("map{element")
        }
        assertThrows<InvalidSyntaxException>("Left brace check failed") {
            Expression.fromString("mapelement}")
        }
    }

    @Test
    fun `fromString invalid CallType`() {
        assertThrows<InvalidSyntaxException> {
            Expression.fromString("mad{element}")
        }
    }

    @Test
    fun `fromString invalid Expression`() {
        assertThrows<InvalidSyntaxException> {
            Expression.fromString("map{(element}")
        }
    }

    @Test
    fun `fromString empty string`() {
        assertThrows<InvalidSyntaxException> {
            Expression.fromString("")
        }
    }

    @Test
    fun `fromString wrong type`() {
        assertThrows<InvalidSyntaxException> {
            Expression.fromString("filter{element}")
        }
    }

    @Test
    fun `fromString correct`() {
        val call = Call.fromString("map{element}")
        assertEquals(CallType.MAP, call.type, "Incorrect call.type parsing")
        assertEquals(
            1, call.expression.tokens.size,
            "Incorrect call.expression parsing: too many tokens"
        )
        assertEquals(ElementToken::class, call.expression.tokens.first()::class)
    }
}

class ExpressionTest {
    @Test
    fun `expressionType ARITHMETIC`() {
        assertEquals(ExpressionType.ARITHMETIC, Expression(ElementToken()).type)
    }

    @Test
    fun `expressionType BOOLEAN`() {
        val expression = Expression(
            listOf(
                ElementToken(),
                NumberToken(3),
                OperationToken(Operation.LESS)
            )
        )
        assertEquals(ExpressionType.BOOLEAN, expression.type)
    }

    @Test
    fun `toString one token`() {
        val expression = Expression(ElementToken())
        assertEquals("element", expression.toString())
    }

    @Test
    fun `toString with operation`() {
        val expression = Expression(
            listOf(
                NumberToken(-3),
                ElementToken(),
                OperationToken(Operation.MULTIPLY),
            )
        )
        assertEquals("(-3*element)", expression.toString())
    }

    @Test
    fun `toString nested operations`() {
        val expression = Expression(
            listOf(
                NumberToken(-3),
                ElementToken(),
                OperationToken(Operation.MULTIPLY),
                NumberToken(1),
                OperationToken(Operation.PLUS),
            )
        )
        assertEquals("((-3*element)+1)", expression.toString())
    }

    @Test
    fun `toString too many operations`() {
        assertThrows<Expression.InvalidStateException> {
            Expression(
                listOf(
                    NumberToken(-3),
                    ElementToken(),
                    OperationToken(Operation.MULTIPLY),
                    OperationToken(Operation.PLUS),
                )
            ).toString()
        }
    }

    @Test
    fun `toString too few operations`() {
        assertThrows<Expression.InvalidStateException> {
            Expression(
                listOf(
                    NumberToken(-3),
                    ElementToken(),
                )
            ).toString()
        }
    }


    @Test
    fun `fromString element`() {
        val expression = Expression.fromString("element")
        assertEquals(expression.tokens.size, 1, "Wrong number of tokens")
        assertEquals(ElementToken::class, expression.tokens.first()::class, "Wrong token class")
    }

    @Test
    fun `fromString number`() {
        val expression = Expression.fromString("-3")
        assertEquals(expression.tokens.size, 1, "Wrong number of tokens")

        val token = expression.tokens.first()
        assertEquals(NumberToken::class, token::class, "Wrong token class")
        assertEquals(-3, (token as NumberToken).number, "NumberToken.number is wrong")
    }

    @Test
    fun `fromString missing parentheses`() {
        assertThrows<InvalidSyntaxException>("Right parenthesis check failed") {
            Expression.fromString("(element*element")
        }
        assertThrows<InvalidSyntaxException>("Left parenthesis check failed") {
            Expression.fromString("element*element)")
        }
    }

    @Test
    fun `fromString empty string`() {
        assertThrows<InvalidSyntaxException> {
            Expression.fromString("")
        }
    }

    @Test
    fun `fromString operation`() {
        val expression = Expression.fromString("(3*element)")
        assertEquals(expression.tokens.size, 3, "Wrong number of tokens")

        val token1 = expression.tokens[0]
        assertEquals(NumberToken::class, token1::class, "Incorrect NumberToken parsing")
        assertEquals(3, (token1 as NumberToken).number, "NumberToken.number is wrong")

        val token2 = expression.tokens[1]
        assertEquals(ElementToken::class, token2::class, "Incorrect ElementToken parsing")

        val token3 = expression.tokens[2]
        assertEquals(OperationToken::class, token3::class, "Incorrect OperationToken parsing")
        assertEquals(
            Operation.MULTIPLY,
            (token3 as OperationToken).operation,
            "Incorrect OperationToken.operation parsing"
        )
    }

    @Test
    fun `fromString too many parenthesis`() {
        assertThrows<InvalidSyntaxException> {
            Expression.fromString("((1+2))")
        }
    }

    @Test
    fun `fromString starts with unary minus`() {
        val expression = Expression.fromString("(-3*element)")
        assertEquals(expression.tokens.size, 3, "Wrong number of tokens")

        val token1 = expression.tokens[0]
        assertEquals(NumberToken::class, token1::class, "Incorrect NumberToken parsing")
        assertEquals(-3, (token1 as NumberToken).number, "NumberToken.number is wrong")

        val token2 = expression.tokens[1]
        assertEquals(ElementToken::class, token2::class, "Incorrect ElementToken parsing")

        val token3 = expression.tokens[2]
        assertEquals(OperationToken::class, token3::class, "Incorrect OperationToken parsing")
        assertEquals(
            Operation.MULTIPLY,
            (token3 as OperationToken).operation,
            "Incorrect OperationToken.operation parsing"
        )
    }

    @Test
    fun `fromString wrong types`() {
        assertThrows<InvalidTypeException> {
            Expression.fromString("((3<5)+1)")
        }
    }
}

class CallTypeTest {
    @Test
    fun `fromString correct typeString`() {
        assertEquals(CallType.MAP, CallType.fromString("map"))
    }

    @Test
    fun `fromString incorrect typeString`() {
        assertThrows<InvalidSyntaxException> {
            CallType.fromString("mad")
        }
    }
}

class OperationTokenTest {
    @Test
    fun `OperationToken expressionType ARITHMETIC`() {
        assertEquals(ExpressionType.ARITHMETIC, OperationToken(Operation.PLUS).expressionType)
    }

    @Test
    fun `OperationToken expressionType BOOLEAN`() {
        assertEquals(ExpressionType.BOOLEAN, OperationToken(Operation.LESS).expressionType)
    }
}

class NumberTokenTest {
    @Test
    fun `NumberToken expressionType`() {
        assertEquals(ExpressionType.ARITHMETIC, NumberToken(1).expressionType)
    }

    @Test
    fun `NumberToken toString`() {
        assertEquals("1", NumberToken(1).toString())
    }
}

class ElementTokenTest {
    @Test
    fun `ElementToken expressionType`() {
        assertEquals(ExpressionType.ARITHMETIC, ElementToken().expressionType)
    }

    @Test
    fun `ElementToken toString`() {
        assertEquals("element", ElementToken().toString())
    }
}

class OperationTest {
    @Test
    fun `verifyOperandTypes correct ExpressionType`() {
        assertDoesNotThrow {
            Operation.PLUS.verifyOperandTypes(ExpressionType.ARITHMETIC, ExpressionType.ARITHMETIC)
        }
    }

    @Test
    fun `verifyOperandTypes incorrect ExpressionType`() {
        assertThrows<InvalidTypeException> {
            Operation.PLUS.verifyOperandTypes(ExpressionType.BOOLEAN, ExpressionType.ARITHMETIC)
        }
    }

    @Test
    fun `fromChar correct operationChar`() {
        assertEquals(Operation.PLUS, Operation.fromChar('+'))
    }

    @Test
    fun `fromChar incorrect operationChar`() {
        assertThrows<InvalidSyntaxException> {
            Operation.fromChar('t')
        }
    }
}