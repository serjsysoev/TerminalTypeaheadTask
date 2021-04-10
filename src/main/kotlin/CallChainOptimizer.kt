import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList

/**
 * Optimizes [callChain] to the [CallChain] that consists of filter call, followed by map call.
 * It also optimizes [call][Call] [expressions][Expression],
 * for example ```((element+10)*(element+10)) -> ((element*element)+((20*element)+100))```.
 *
 * @param callChain [CallChain] to optimize
 *
 * @return optimized [CallChain]
 */
fun optimizeCallChain(callChain: CallChain): CallChain {
    var filterCall: Call? = null
    var mapCallPolynomialToken = PolynomialToken(arrayListOf(0, 1)) // map element -> element

    for (call in callChain.calls) {
        val callTokens = call.expression.tokens.toMutableList()
        callTokens.replaceAll { if (it is ElementToken) mapCallPolynomialToken else it }

        when (call.type) {
            CallType.FILTER -> {
                val oldFilterTokensWithAnd =
                    filterCall?.expression?.tokens?.plus(OperationToken(Operation.AND)) ?: emptyList()

                val newFilterTokens = callTokens + oldFilterTokensWithAnd
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
    return CallChain(calls)
}

private val emptyFilter = Call( // filter{(1=1)}
    CallType.FILTER,
    Expression(listOf(NumberToken(1), NumberToken(1), OperationToken(Operation.EQUAL)))
)

/**
 * [Token] that represents a polynomial where polynomial variable is element
 */
class PolynomialToken : Token {
    override val expressionType: ExpressionType
        get() = ExpressionType.ARITHMETIC

    private val polynomial: ArrayList<Int>

    /**
     * Constructs [PolynomialToken] from an ArrayList of coefficients
     *
     * @param polynomial [ArrayList] of coefficients starting from the lowest degree
     */
    constructor(polynomial: ArrayList<Int> = arrayListOf()) {
        this.polynomial = polynomial
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
                is NumberToken -> polynomialTokenStack.add(PolynomialToken(arrayListOf(it.number)))
                is ElementToken -> polynomialTokenStack.add(PolynomialToken(arrayListOf(0, 1)))
                is PolynomialToken -> polynomialTokenStack.add(it)
            }
        }
        if (polynomialTokenStack.size != 1) throw IllegalArgumentException()
        polynomial = polynomialTokenStack.first.polynomial
    }

    /**
     * Construct a new polynomial that equals operation(this, [polynomialToken])
     *
     * @param operation [Operation] to perform
     * @param polynomialToken second parameter for the operation
     *
     * @return result of operation(this, [polynomialToken])
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
        val answer = PolynomialToken()
        for (i in 0 until maxOf(this.polynomial.size, that.polynomial.size)) {
            answer.polynomial.add(
                this.polynomial.getOrElse(i) { 0 } + that.polynomial.getOrElse(i) { 0 }
            )
        }

        return answer
    }

    /**
     * Returns difference of the polynomials
     */
    operator fun minus(that: PolynomialToken): PolynomialToken {
        val answer = PolynomialToken()
        for (i in 0 until maxOf(this.polynomial.size, that.polynomial.size)) {
            answer.polynomial.add(
                this.polynomial.getOrElse(i) { 0 } - that.polynomial.getOrElse(i) { 0 }
            )
        }

        return answer
    }

    /**
     * Returns product of the polynomials
     */
    operator fun times(that: PolynomialToken): PolynomialToken {
        val answer = PolynomialToken()
        for (i in 0 until this.polynomial.size) {
            for (j in 0 until that.polynomial.size) {
                while (answer.polynomial.size < i + j + 1) answer.polynomial.add(0)

                answer.polynomial[i + j] += this.polynomial[i] * that.polynomial[j]
            }
        }

        return answer
    }

    private fun <T> ArrayList<T>.forEachIndexedReversed(block: (Int, T) -> Unit) {
        for (i in size-1 downTo 0) {
            block(i, this[i])
        }
    }

    /**
     * Converts [PolynomialToken] to the expression equivalent of this polynomial
     * that does not contain [polynomial tokens][PolynomialToken]
     */
    fun toExpression(): Expression {
        val tokensList = mutableListOf<Token>()

        var nonZeroCoefficientsCount = 0

        polynomial.forEachIndexedReversed { index, coefficient ->
            if (coefficient == 0) return@forEachIndexedReversed
            nonZeroCoefficientsCount++

            if (coefficient == 1) {
                repeat(index) { tokensList.add(ElementToken()) }
                repeat(maxOf(index - 1, 0)) { tokensList.add(OperationToken(Operation.MULTIPLY)) }
            } else {
                tokensList.add(NumberToken(coefficient))
                repeat(index) { tokensList.add(ElementToken()) }
                repeat(index) { tokensList.add(OperationToken(Operation.MULTIPLY)) }
            }
        }

        repeat(nonZeroCoefficientsCount - 1) { tokensList.add(OperationToken(Operation.PLUS)) }

        return Expression(tokensList)
    }

    override fun toString(): String {
        return toExpression().toString()
    }
}
