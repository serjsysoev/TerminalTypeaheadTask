fun main() {
    val callChainString = readLine() ?: ""

    val callChain = try {
        CallChain.fromString(callChainString)
    } catch (invalidSyntaxException: InvalidSyntaxException) {
        println("SYNTAX ERROR")
        return
    } catch (invalidTypeException: InvalidTypeException) {
        println("TYPE ERROR")
        return
    }

    val optimizedCallChain = optimizeCallChain(callChain)

    print(optimizedCallChain)
}