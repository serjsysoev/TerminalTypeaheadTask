# Call-chain optimizer

This program optimizes call-chain that consists of multiple filter/map calls 
down to one filter call followed by a map call. It puts polynomials into standard form.

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

* For arithmetic operators ("+", "-", "*"), input and output types are "number"
* For comparison operators ("<", ">", "="), input type is "number", output type is boolean
* For logical operators ("&", "|"), input and output types are boolean

If call-chain syntax is incorrect, the program prints "SYNTAX ERROR",
if operand type does not match the expected input type it prints "TYPE ERROR",
otherwise it prints call-chain that consists of one filter call followed by a map call.

# Testing
You can run tests by ```./gradlew(.bat) test```

To get coverage results from jacoco run ```./gradlew(.bat) jacocoTestReport```.
This will produce html page and a xml file in build/reports/jacoco/test folder.

**NOTE:** run tests before executing jacocoTestReport