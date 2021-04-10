# Call-chain optimizer

This program optimizes call-chain that consists of multiple filter/map calls 
down to one filter call followed by a map call.

## Usage
Run ```./gradlew(.bat) run --console=plain```

You need to pass the call-chain string representation to the input stream
in the following format:
```
<digit>   ::= “0” | “1" | “2” | “3" | “4” | “5" | “6” | “7" | “8” | “9"
<number> ::= <digit> | <digit> <number>
<operation> ::= “+” | “-” | “*” | “>” | “<” | “=” | “&” | “|”
<constant-expression> ::= “-” <number> | <number>
<binary-expression> ::= “(” <expression> <operation> <expression> “)”
<expression> ::= “element” | <constant-expression> | <binary-expression>
<map-call> ::= “map{” <expression> “}”
<filter-call> ::= “filter{” <expression> “}”
<call> ::= <map-call> | <filter-call>
<call-chain> ::= <call> | <call> “%>%” <call-chain>
```

Arithmetic operators ("+", "-", "*") input and output types are number, 
comparison operators ("<", ">", "=") input type is number, output type is boolean, 
logical operators ("&", "|") input and output types are boolean.

If call-chain syntax is incorrect, the program prints "SYNTAX ERROR",
if operand type does not match the expected input type it prints "TYPE ERROR",
otherwise it prints call-chain that consists of one filter call followed by a map call.