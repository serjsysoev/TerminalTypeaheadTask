import java.lang.Integer.parseInt
import java.util.*
import java.util.regex.Pattern
import kotlin.Exception

/**
 * Represents a call-chain (see README.md)
 *
 * @property calls list of [calls][Call] call-chain consists of
 *
 * @constructor creates a new CallChain instance
 */
data class CallChain(val calls: List<Call>) {
    /**
     * Returns string representation of the call-chain as described in README.md
     */
    override fun toString(): String {
        return calls.joinToString("%>%")
    }

    companion object {
        /**
         * Parses string representation of call-chain to [CallChain] object.
         *
         * @param callChainString string representation of [CallChain]
         *
         * @throws InvalidSyntaxException if call-chain string representation is incorrect and cannot be parsed
         * @throws InvalidTypeException if operands of operator/call does not match its input types
         */
        fun fromString(callChainString: String): CallChain {
            val callStrings = callChainString.split("%>%").filter { it.isNotEmpty() }
            if (callStrings.isEmpty()) throw InvalidSyntaxException()

            val calls = callStrings.map { Call.fromString(it) }
            return CallChain(calls)
        }
    }
}

/**
 * Represents a call (see README.md)
 *
 * @property type [type][CallType] of your call
 * @property expression [expression][Expression] that a call executes
 *
 * @constructor creates a new Call instance
 * @throws InvalidTypeException if [type].expressionType does not match [expression].type
 */
data class Call(
    val type: CallType,
    val expression: Expression,
) {
    init {
        if (type.expressionType != expression.type) {
            throw InvalidTypeException()
        }
    }

    /**
     * Returns string representation of the call as described in README.md
     */
    override fun toString(): String {
        return "${type.typeString}{$expression}"
    }

    companion object {
        /**
         * Parses string representation of call to [Call] object.
         *
         * @param callString string representation of [Call]
         *
         * @throws InvalidSyntaxException if call string representation is incorrect and cannot be parsed
         * @throws InvalidTypeException if operands of operator/call does not match its input types
         */
        fun fromString(callString: String): Call {
            val callMatcher = callPattern.matcher(callString)
            if (!callMatcher.find() || callMatcher.groupCount() != 2) throw InvalidSyntaxException()

            val typeString = callMatcher.group(1)
            val expressionString = callMatcher.group(2)

            return Call(
                CallType.fromString(typeString),
                Expression.fromString(expressionString)
            )
        }

        private val callPattern = Pattern.compile("""^(filter|map)\{(.*)}$""")
    }
}

/**
 * Represents an expression (see README.md)
 *
 * @property tokens list of [tokens][Token] in a reverse-polish notation.
 * The expression should always be correct.
 * If expression is not executable .toString may throw an exception or produce invalid expression string
 */
class Expression(val tokens: List<Token>) {
    /**
     * [ExpressionType] of the whole expression
     */
    val type: ExpressionType = tokens.last().expressionType

    /**
     * Constructs [Expression] from a single token
     */
    constructor(token: Token) : this(listOf(token))

    /**
     * Returns string representation of the expression as described in README.md
     *
     * @throws InvalidStateException if expression is invalid and cannot be casted to a string
     */
    override fun toString(): String {
        val stringsStack = LinkedList<String>()

        for (token in tokens) {
            if (token is OperationToken) {
                val secondOperand = stringsStack.removeLastOrNull() ?: throw InvalidStateException()
                val firstOperand = stringsStack.removeLast() ?: throw InvalidStateException()

                stringsStack.add("($firstOperand${token.operation.operationChar}$secondOperand)")
            } else stringsStack.add(token.toString())
        }

        if (stringsStack.size != 1) throw InvalidStateException()
        return stringsStack.first
    }

    companion object {
        /**
         * Parses string representation of expression to [Expression] object.
         *
         * @param expressionString string representation of [Expression]
         *
         * @throws InvalidSyntaxException if expression string representation is incorrect and cannot be parsed
         * @throws InvalidTypeException if operands of operator does not match its input types
         */
        fun fromString(expressionString: CharSequence): Expression {
            if (expressionString == "element") return Expression(ElementToken())

            return try {
                // this function could be optimized by using parseInt for CharSequence
                // and using CharSequence implementation that doesn't copy underlying string on subSequence calls
                val number = parseInt(expressionString.toString())

                Expression(NumberToken(number))
            } catch (ex: Exception) {
                if (expressionString.first() != '(' || expressionString.last() != ')') throw InvalidSyntaxException()
                val expressionWithoutBrackets = expressionString.subSequence(1, expressionString.length - 1)


                val operationPosition = findOperationPosition(expressionWithoutBrackets)
                val operation = Operation.fromChar(expressionWithoutBrackets[operationPosition])
                val operationToken = OperationToken(operation)

                val leftPartString = expressionWithoutBrackets.subSequence(0, operationPosition)
                val rightPartString = expressionWithoutBrackets.subSequence(
                    operationPosition + 1,
                    expressionWithoutBrackets.length
                )

                val leftPart = fromString(leftPartString)
                val rightPart = fromString(rightPartString)

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

            throw InvalidSyntaxException()
        }
    }

    /**
     * This exception is thrown if user interactions with the object broke the contract.
     * You should not rely on this exception as it is not guaranteed to be thrown
     * if the object is in the invalid state.
     */
    class InvalidStateException : Exception()
}

/**
 * Enum of types of calls that can be used in a call-chain
 *
 * @property typeString string representation of the CallType
 * @property expressionType [ExpressionType] that the expression evaluation wold produce
 */
enum class CallType(val typeString: String, val expressionType: ExpressionType) {
    MAP("map", ExpressionType.ARITHMETIC), FILTER("filter", ExpressionType.BOOLEAN);

    companion object {
        private val typeStringToOperation = values().map { it.typeString to it }.toMap()

        /**
         * returns [CallType] by its [typeString]
         *
         * @param typeString string representation of the [CallType]
         *
         * @throws InvalidSyntaxException there is no [CallType] that is represented by this [typeString]
         */
        fun fromString(typeString: String): CallType = typeStringToOperation.getOrElse(typeString) {
            throw InvalidSyntaxException()
        }
    }
}

/**
 * Enum of the types that expression evaluation would produce
 */
enum class ExpressionType {
    BOOLEAN, ARITHMETIC,
}

/**
 * Building block of the [Expression]
 */
abstract class Token {
    /**
     * [ExpressionType] of expression if its last operation was this token
     */
    abstract val expressionType: ExpressionType
}

/**
 * [Token] that represents an operation
 *
 * @property operation [Operation] that is represented by this token
 */
data class OperationToken(
    val operation: Operation,
) : Token() {
    override val expressionType: ExpressionType
        get() = operation.expressionOutputType
}

/**
 * [Token] that represents a number
 *
 * @property number number that is represented by this token
 */
data class NumberToken(
    val number: Int
) : Token() {
    override val expressionType: ExpressionType
        get() = ExpressionType.ARITHMETIC

    override fun toString(): String {
        return number.toString()
    }
}

/**
 * [Token] that represents an element
 */
class ElementToken : Token() {
    override val expressionType: ExpressionType
        get() = ExpressionType.ARITHMETIC

    override fun toString(): String {
        return "element"
    }
}

/**
 * Enum of operations from README.md
 *
 * @property operationChar string representation of the operation
 * @property operandExpressionType [ExpressionType] of operands supported by the operation
 * @property expressionOutputType [ExpressionType] that this operation produces
 */
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

    /**
     * Verifies that operands [expression types][ExpressionType] are supported by this operation
     */
    fun verifyOperandTypes(operand1: ExpressionType, operand2: ExpressionType) {
        if (operand1 != operandExpressionType || operand2 != operandExpressionType) {
            throw InvalidTypeException()
        }
    }

    companion object {
        private val charToOperation = values().map { it.operationChar to it }.toMap()

        /**
         * Returns [Operation] by its [operationChar]
         *
         * @param operationChar char representation of the [Operation]
         *
         * @throws InvalidSyntaxException there is no [Operation] that is represented by this [operationChar]
         */
        fun fromChar(operationChar: Char): Operation = charToOperation.getOrElse(operationChar) {
            throw InvalidSyntaxException()
        }
    }
}

/**
 * This exception is thrown when the CallChain string representation syntax is invalid
 */
class InvalidSyntaxException : Exception()

/**
 * This exception is thrown if the operands of the operation/function are not of the expected type
 */
class InvalidTypeException : Exception()
