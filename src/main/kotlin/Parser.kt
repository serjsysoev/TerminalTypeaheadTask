import java.lang.Integer.parseInt
import java.util.*
import java.util.regex.Pattern
import kotlin.Exception

fun parseCallChain(callChain: String): CallChain {
    val callStrings = callChain.split("%>%").filter { it.isNotEmpty() }
    if (callStrings.isEmpty()) throw SyntaxErrorException()

    val calls = callStrings.map { parseCall(it) }
    return CallChain(calls)
}

private val callPattern = Pattern.compile("""^(filter|map)\{(.*)}$""")
private fun parseCall(call: String): Call {
    val callMatcher = callPattern.matcher(call)
    if (!callMatcher.find() || callMatcher.groupCount() != 2) throw SyntaxErrorException()

    val typeString = callMatcher.group(1)
    val expressionString = callMatcher.group(2)

    return Call(
        CallType.getCallTypeByString(typeString),
        parseExpression(expressionString)
    )
}

private fun parseExpression(expression: CharSequence): Expression {
    if (expression == "element") return Expression(ElementToken())

    return try {
        val number = parseInt(expression, 0, expression.length, 10)

        Expression(NumberToken(number))
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
        return Expression(leftPart.tokens + rightPart.tokens + operationToken)
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

data class CallChain(val calls: List<Call>) {
    override fun toString(): String {
        return calls.joinToString("%>%")
    }
}

data class Call(
    val type: CallType,
    val expression: Expression,
) {
    init {
        if (type.expressionType != expression.type) {
            throw TypeErrorException()
        }
    }

    override fun toString(): String {
        return "${type.typeString}{$expression}"
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

class Expression(
    /**
     * Tokens in reverse Polish notation.
     */
    val tokens: List<Token>,
) {
    constructor(token: Token) : this(listOf(token))

    val type: ExpressionType = tokens.last().expressionType

    override fun toString(): String {
        val stringsStack = LinkedList<String>()

        for (token in tokens) {
            if (token is OperationToken) {
                val secondOperand = stringsStack.removeLast()
                val firstOperand = stringsStack.removeLast()

                stringsStack.add("($firstOperand${token.operation.operationChar}$secondOperand)")
            } else stringsStack.add(token.toString())
        }

        if (stringsStack.size != 1) TODO("Maybe I need to guarantee this wouldn't happen")
        return stringsStack.first
    }
}

enum class ExpressionType {
    BOOLEAN, ARITHMETIC,
}

abstract class Token {
    abstract val expressionType: ExpressionType
}

data class OperationToken(
    val operation: Operation,
) : Token() {
    override val expressionType: ExpressionType
        get() = operation.expressionOutputType
}

data class NumberToken(
    val number: Int
) : Token() {
    override val expressionType: ExpressionType
        get() = ExpressionType.ARITHMETIC

    override fun toString(): String {
        return number.toString()
    }
}

class ElementToken : Token() {
    override val expressionType: ExpressionType
        get() = ExpressionType.ARITHMETIC

    override fun toString(): String {
        return "element"
    }
}

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
