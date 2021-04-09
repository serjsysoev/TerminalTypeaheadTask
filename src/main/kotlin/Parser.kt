import java.lang.Integer.parseInt
import kotlin.Exception

fun parseCallChain(callChain: String): CallChain {
    val callStrings = callChain.split("%>%").filter { it.isNotEmpty() }
    if (callStrings.isEmpty()) throw SyntaxErrorException()

    return callStrings.map { parseCall(it) }
}

private fun parseCall(call: String): Call {
    val strings = call.split("{", "}").filter { it.isNotEmpty() }
    if (strings.size > 2) throw SyntaxErrorException()

    val (typeString, expressionString) = strings

    return Call(
        CallType.getCallTypeByString(typeString),
        parseExpression(expressionString)
    )
}

private fun parseExpression(expression: CharSequence): Expression {
    if (expression == "element") return Expression(listOf(ElementToken()), ExpressionType.ARITHMETIC)

    return try {
        val number = parseInt(expression, 0, expression.length, 10)

        Expression(listOf(NumberToken(number)), ExpressionType.ARITHMETIC)
    } catch (ex: Exception) {
        if (expression.first() != '(' || expression.last() != ')') throw SyntaxErrorException()
        val expressionWithoutBrackets = expression.subSequence(1, expression.length - 1)


        val operationPosition = findOperationPosition(expressionWithoutBrackets)
        val operation = Operation.getOperationByChar(expressionWithoutBrackets[operationPosition])
        val operationToken = OperationToken(operation)

        val leftPartString = expressionWithoutBrackets.subSequence(0, operationPosition)
        val rightPartString = expressionWithoutBrackets.subSequence(
            operationPosition + 1,
            expressionWithoutBrackets.length
        )

        val leftPart = parseExpression(leftPartString)
        val rightPart = parseExpression(rightPartString)

        operation.verifyOperandTypes(leftPart.type, rightPart.type)
        return Expression(leftPart.tokens + rightPart.tokens + operationToken, operation.expressionOutputType)
    }
}

private fun findOperationPosition(expression: CharSequence): Int {
    var openBracketsCount = 0

    expression.forEachIndexed { index, char ->
        when (char) {
            '(' -> openBracketsCount++
            ')' -> openBracketsCount--
            in Operation.values().map { it.operationChar } -> if (openBracketsCount == 0) return index
        }
    }

    throw SyntaxErrorException()
}

typealias CallChain = List<Call>

data class Call(
    val type: CallType,
    val expression: Expression,
) {
    init {
        if (type.expressionType != expression.type) {
            throw TypeErrorException()
        }
    }
}

enum class CallType(val typeString: String, val expressionType: ExpressionType) {
    MAP("map", ExpressionType.ARITHMETIC), FILTER("filter", ExpressionType.BOOLEAN);

    companion object {
        private val typeStringToOperation = values().map { it.typeString to it }.toMap()

        fun getCallTypeByString(typeString: String): CallType = typeStringToOperation.getOrElse(typeString) {
            throw SyntaxErrorException()
        }
    }
}

data class Expression(
    /**
     * Tokens in reverse Polish notation.
     */
    val tokens: List<Token>,
    val type: ExpressionType,
)

enum class ExpressionType {
    BOOLEAN, ARITHMETIC,
}

interface Token

data class OperationToken(
    val operation: Operation,
) : Token

data class NumberToken(
    val number: Int
) : Token

class ElementToken : Token

enum class Operation(
    val operationChar: Char,
    val operandExpressionType: ExpressionType,
    val expressionOutputType: ExpressionType
) {
    PLUS('+', ExpressionType.ARITHMETIC, ExpressionType.ARITHMETIC),
    MINUS('-', ExpressionType.ARITHMETIC, ExpressionType.ARITHMETIC),
    MULTIPLY('*', ExpressionType.ARITHMETIC, ExpressionType.ARITHMETIC),
    LESS('<', ExpressionType.ARITHMETIC, ExpressionType.BOOLEAN),
    GREATER('>', ExpressionType.ARITHMETIC, ExpressionType.BOOLEAN),
    EQUAL('=', ExpressionType.ARITHMETIC, ExpressionType.BOOLEAN),
    AND('&', ExpressionType.BOOLEAN, ExpressionType.BOOLEAN),
    OR('|', ExpressionType.BOOLEAN, ExpressionType.BOOLEAN);

    fun verifyOperandTypes(operand1: ExpressionType, operand2: ExpressionType) {
        if (operand1 != operandExpressionType || operand2 != operandExpressionType) {
            throw TypeErrorException()
        }
    }

    companion object {
        private val charToOperation = values().map { it.operationChar to it }.toMap()

        fun getOperationByChar(operationChar: Char): Operation = charToOperation.getOrElse(operationChar) {
            throw SyntaxErrorException()
        }
    }
}

class SyntaxErrorException : Exception()
class TypeErrorException : Exception()
