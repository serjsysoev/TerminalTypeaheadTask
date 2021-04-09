import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList

fun optimizeCallChain(callChain: CallChain): CallChain {
    var filterCall: Call? = null
    var mapCallPolynomialToken = PolynomialToken(arrayListOf(0, 1)) // map element -> element

    for (call in callChain) {
        val callTokens = call.expression.tokens.toMutableList()
        callTokens.replaceAll { if (it is ElementToken) mapCallPolynomialToken else it }

        when (call.type) {
            CallType.FILTER -> {
                val oldFilterTokens = filterCall?.expression?.tokens ?: emptyList()
                val newFilterTokens = oldFilterTokens + callTokens + OperationToken(Operation.AND)
                filterCall = Call(CallType.FILTER, Expression(newFilterTokens))
            }
            CallType.MAP -> {
                mapCallPolynomialToken = convertArithmeticExpressionToPolynomialToken(Expression(callTokens))
            }
        }
    }

    return listOf(
        filterCall ?: emptyFilter,
        Call(CallType.MAP, Expression(mapCallPolynomialToken))
    )
}

private val emptyFilter = Call( // filter{(1=1)}
    CallType.FILTER,
    Expression(listOf(NumberToken(1), NumberToken(1), OperationToken(Operation.EQUAL)))
)

private fun convertArithmeticExpressionToPolynomialToken(expression: Expression): PolynomialToken {
    val polynomialTokenStack = LinkedList<PolynomialToken>()
    expression.tokens.forEach {
        when (it) {
            is OperationToken -> {
                val secondPolynomial = polynomialTokenStack.pop()
                val firstPolynomial = polynomialTokenStack.pop()
                polynomialTokenStack.add(firstPolynomial.performOperation(it.operation, secondPolynomial))
            }
            is NumberToken -> polynomialTokenStack.add(PolynomialToken(arrayListOf(it.number)))
            is ElementToken -> polynomialTokenStack.add(PolynomialToken(arrayListOf(0, 1)))
            is PolynomialToken -> polynomialTokenStack.add(it)
        }
    }
    if (polynomialTokenStack.size != 1) throw IllegalArgumentException()
    return polynomialTokenStack.first
}

data class PolynomialToken(
    val polynomial: ArrayList<Int> = arrayListOf()
) : Token() {
    override val expressionType: ExpressionType
        get() = ExpressionType.ARITHMETIC

    fun performOperation(operation: Operation, polynomial: PolynomialToken) =
        when (operation) {
            Operation.PLUS -> this + polynomial
            Operation.MINUS -> this - polynomial
            Operation.MULTIPLY -> this * polynomial
            else -> throw TypeErrorException()
        }

    operator fun plus(that: PolynomialToken): PolynomialToken {
        val answer = PolynomialToken()
        for (i in 0 until maxOf(this.polynomial.size, that.polynomial.size)) {
            answer.polynomial.add(
                this.polynomial.getOrElse(i) { 0 } + that.polynomial.getOrElse(i) { 0 }
            )
        }

        return answer
    }

    operator fun minus(that: PolynomialToken): PolynomialToken {
        val answer = PolynomialToken()
        for (i in 0 until maxOf(this.polynomial.size, that.polynomial.size)) {
            answer.polynomial.add(
                this.polynomial.getOrElse(i) { 0 } - that.polynomial.getOrElse(i) { 0 }
            )
        }

        return answer
    }

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
}
