# Call-chain optimizer

This program optimizes call-chain that consists of multiple filter/map calls 
down to one filter call followed by a map call.

## Usage
//TODO Run ```./gradlew(.bat) run```

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

Arithmetic operators ("+", "-", "*") input and output types are number.

Comparison operators ("<", ">", "=") input type is number, output type is boolean.

Logical operators ("&", "|") input and output types are boolean.