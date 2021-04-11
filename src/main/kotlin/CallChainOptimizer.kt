import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * Optimizes [callChain] to the [CallChain] that consists of filter call, followed by map call.
 * It also optimizes [call][Call] [expressions][Expression],
 * for example ```((element+10)*(element+10)) -> ((element*element)+((20*element)+100))```.
 *
 * @param callChain - [CallChain] to optimize
 *
 * @return optimized [CallChain]
 *
 * @throws IllegalArgumentException if callChain is malformed and cannot be processed
 *
 */
fun optimizeCallChain(callChain: CallChain): CallChain = try {
    var filterCall: Call? = null
    var mapCallPolynomialToken = PolynomialToken(listOf(0, 1)) // map element -> element

    for (call in callChain.calls) {
        val callTokens = call.expression.tokens.toMutableList()
        callTokens.replaceAll { if (it is ElementToken) mapCallPolynomialToken else it }

        when (call.type) {
            CallType.FILTER -> {
                val oldFilterTokensWithAnd =
                    filterCall?.expression?.tokens?.plus(OperationToken(Operation.AND)) ?: emptyList()

                val newFilterTokens = optimizeFilterTokens(callTokens) + oldFilterTokensWithAnd
                filterCall = Call(CallType.FILTER, Expression(newFilterTokens))
            }
            CallType.MAP -> {
                mapCallPolynomialToken = PolynomialToken(Expression(callTokens))
            }
        }
    }

    val calls = listOf(
        filterCall ?: emptyFilter,
        Call(CallType.MAP, Expression(mapCallPolynomialToken))
    )

    CallChain(calls)
} catch (ex: Exception) {
    throw IllegalArgumentException()
}

private fun optimizeFilterTokens(tokens: List<Token>): List<Token> {
    val answer = LinkedList<Token>()

    for (token in tokens) {
        if (token !is OperationToken) {
            answer.add(PolynomialToken(Expression(token)))
            continue
        }

        val operation = token.operation

        // this could be further optimized
        if (operation.operandExpressionType == ExpressionType.BOOLEAN) {
            answer.add(token)
            continue
        }

        val secondPolynomialToken = answer.removeLast() as PolynomialToken
        val firstPolynomialToken = answer.removeLast() as PolynomialToken

        if (operation.expressionOutputType == ExpressionType.ARITHMETIC) {
            answer.add(firstPolynomialToken.performOperation(operation, secondPolynomialToken))
        } else {
            answer.addAll(optimizeComparisonOperator(firstPolynomialToken, secondPolynomialToken, operation))
        }
    }

    return answer
}

private fun optimizeComparisonOperator(
    firstPolynomialToken: PolynomialToken,
    secondPolynomialToken: PolynomialToken,
    operation: Operation
): List<Token> {
    if (operation == Operation.GREATER) {
        return optimizeComparisonOperator(secondPolynomialToken, firstPolynomialToken, Operation.LESS)
    }

    val result = secondPolynomialToken - firstPolynomialToken

    if (operation == Operation.EQUAL && result.polynomial.size == 1) {
        return listOf(
            NumberToken(if (result.polynomial.first() == 0) 1 else 0),
            NumberToken(1),
            OperationToken(Operation.EQUAL)
        )
    }

    if (operation == Operation.LESS && result.polynomial.size == 1) {
        return listOf(
            NumberToken(if (result.polynomial.first() > 0) 1 else 0),
            NumberToken(1),
            OperationToken(Operation.EQUAL)
        )
    }

    return listOf(PolynomialToken(), result, OperationToken(operation))
}

private val emptyFilter = Call( // filter{(1=1)}
    CallType.FILTER,
    Expression(listOf(NumberToken(1), NumberToken(1), OperationToken(Operation.EQUAL)))
)

/**
 * [Token] that represents a polynomial where polynomial variable is element
 */
class PolynomialToken : Token {
    override val expressionType: ExpressionType = ExpressionType.ARITHMETIC

    private val mutablePolynomial: ArrayList<Int>

    val polynomial: List<Int>
        get() = mutablePolynomial

    /**
     * Constructs [PolynomialToken] from a list of coefficients
     *
     * @param polynomialIndices list of coefficients starting from the lowest degree
     *
     * @throws IllegalArgumentException if [polynomialIndices] is empty
     */
    constructor(polynomialIndices: List<Int> = listOf(0)) {
        if (polynomialIndices.isEmpty()) throw IllegalArgumentException()
        mutablePolynomial = ArrayList(polynomialIndices)
        removeZeroes()
    }

    /**
     * Constructs [PolynomialToken] from an Expression
     */
    constructor(expression: Expression) {
        val polynomialTokenStack = LinkedList<PolynomialToken>()
        expression.tokens.forEach {
            when (it) {
                is OperationToken -> {
                    val secondPolynomial = polynomialTokenStack.removeLast()
                    val firstPolynomial = polynomialTokenStack.removeLast()
                    polynomialTokenStack.add(firstPolynomial.performOperation(it.operation, secondPolynomial))
                }
                is NumberToken -> polynomialTokenStack.add(PolynomialToken(listOf(it.number)))
                is ElementToken -> polynomialTokenStack.add(PolynomialToken(listOf(0, 1)))
                is PolynomialToken -> polynomialTokenStack.add(it)
            }
        }
        if (polynomialTokenStack.size != 1) throw IllegalArgumentException()
        mutablePolynomial = polynomialTokenStack.first.mutablePolynomial
    }

    /**
     * Construct a new polynomial that equals this "x" [polynomialToken], where "x" is the operation
     *
     * @param operation [Operation] to perform
     * @param polynomialToken second parameter for the operation
     *
     * @return result of this "x" [polynomialToken]
     *
     * @throws InvalidTypeException if operation input or output type doesn't match [ExpressionType.ARITHMETIC]
     */
    fun performOperation(operation: Operation, polynomialToken: PolynomialToken) =
        when (operation) {
            Operation.PLUS -> this + polynomialToken
            Operation.MINUS -> this - polynomialToken
            Operation.MULTIPLY -> this * polynomialToken
            else -> throw InvalidTypeException()
        }

    /**
     * Returns sum of the polynomials
     */
    operator fun plus(that: PolynomialToken): PolynomialToken {
        val polynomialIndices = mutableListOf<Int>()
        for (i in 0 until maxOf(this.polynomial.size, that.polynomial.size)) {
            polynomialIndices.add(
                this.polynomial.getOrElse(i) { 0 } + that.polynomial.getOrElse(i) { 0 }
            )
        }

        removeZeroes()
        return PolynomialToken(polynomialIndices)
    }

    /**
     * Returns difference of the polynomials
     */
    operator fun minus(that: PolynomialToken): PolynomialToken {
        val polynomialIndices = mutableListOf<Int>()
        for (i in 0 until maxOf(this.polynomial.size, that.polynomial.size)) {
            polynomialIndices.add(
                this.polynomial.getOrElse(i) { 0 } - that.polynomial.getOrElse(i) { 0 }
            )
        }

        removeZeroes()
        return PolynomialToken(polynomialIndices)
    }

    /**
     * Returns product of the polynomials
     */
    operator fun times(that: PolynomialToken): PolynomialToken {
        val polynomialIndices = mutableListOf<Int>()
        for (i in this.polynomial.indices) {
            for (j in that.polynomial.indices) {
                while (polynomialIndices.size < i + j + 1) polynomialIndices.add(0)

                polynomialIndices[i + j] += this.polynomial[i] * that.polynomial[j]
            }
        }

        removeZeroes()
        return PolynomialToken(polynomialIndices)
    }

    /**
     * Converts [PolynomialToken] to the expression equivalent of this polynomial
     * that does not contain [polynomial tokens][PolynomialToken]
     */
    fun toExpression(): Expression {
        val tokensList = mutableListOf<Token>()

        val signTokens = mutableListOf<OperationToken>()

        mutablePolynomial.forEachIndexedReversed { index, coefficient ->
            if (coefficient == 0) return@forEachIndexedReversed

            if (abs(coefficient) == 1 && index > 0) {
                repeat(index) { tokensList.add(ElementToken()) }
                repeat(maxOf(index - 1, 0)) { tokensList.add(OperationToken(Operation.MULTIPLY)) }
            } else {
                tokensList.add(NumberToken(abs(coefficient)))
                repeat(index) { tokensList.add(ElementToken()) }
                repeat(index) { tokensList.add(OperationToken(Operation.MULTIPLY)) }
            }

            if (index != mutablePolynomial.lastIndex) {
                if (coefficient < 0) {
                    tokensList.add(OperationToken(Operation.MINUS))
                } else {
                    tokensList.add(OperationToken(Operation.PLUS))
                }
            }
        }

        tokensList.addAll(signTokens)

        if (tokensList.isEmpty()) tokensList.add(NumberToken(0))
        return Expression(tokensList)
    }

    override fun toString(): String {
        return toExpression().toString()
    }

    private fun <T> List<T>.forEachIndexedReversed(block: (Int, T) -> Unit) {
        for (i in size - 1 downTo 0) {
            block(i, this[i])
        }
    }

    private fun removeZeroes() {
        while (mutablePolynomial.size > 1 && mutablePolynomial.last() == 0)
            mutablePolynomial.removeAt(mutablePolynomial.lastIndex)
    }
}
