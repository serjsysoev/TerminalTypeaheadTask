fun main() {
    val callChainString = readLine() ?: ""

    val callChain = try {
        parseCallChain(callChainString)
    } catch (syntaxError: SyntaxErrorException) {
        println("SYNTAX ERROR")
        return
    } catch (typeError: TypeErrorException) {
        println("TYPE ERROR")
        return
    }

    val optimizedCallChain = optimizeCallChain(callChain)

    print(optimizedCallChain)
}