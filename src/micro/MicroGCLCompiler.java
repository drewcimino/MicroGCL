package micro;

/* MicroGCL grammar
<system goal>	-> #Start <program> EofSym #Finish
<program>		-> "BEGIN" <statement list> "END" "." 
<statement list>-> <statement> {<statement>}
<statement>		-> <variable> ":=" <expression> #Assign ";"
<statement>     -> "READ"  <var list>  ";"
<statement>		-> "WRITE"  <expr list>  ";" #EndWrite
<statement>		-> "IF" #StartIf<guard> "->" <statement list> "[]" <statement list> "FI "";"
<statement>		-> "SKIP"";"
<var list>      -> <variable> #ReadVar {"," <variable> #ReadVar }
<expr list>		-> <expression> #WriteExpr {"," <expression> #WriteExpr}
<guard>			-> <expression> <rel op> <expression>
<expression>    -> <factor> {<add op> <factor> #AddExpression}
<factor> 	  	-> <primary> {<mult op> <primary> #MultiplyExpression }
<primary>		-> "(" <expression>")"
<primary>		-> <variable>
<primary>		-> "IntLiteral" #ProcessLiteral
<rel op>		-> ("=" | "#" | "<" | "<=" | ">" | ">=")  #ProcessRelOp
<add op>		-> ( "+" | "-" )  #ProcessOp
<mult op>      	-> ("*" | "/")  #ProcessMultOp
<variable>      -> "IDENTIFIER" #ProcessIdentifier
	*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

// ------------------------------ Token ----------------------------
class Token {
	public static final Token IDENTIFIER 			= new Token("identifier");// 0
	public static final Token INTEGER_LITERAL 		= new Token("integer literal");// 1
	public static final Token ASSIGN_OPERATOR 		= new Token(":=");// 2
	public static final Token PLUS_OPERATOR 		= new Token("+");// 3
	public static final Token MINUS_OPERATOR 		= new Token("-");// 4
	public static final Token COMMA 				= new Token(",");// 5
	public static final Token SEMICOLON 			= new Token(";");// 6
	public static final Token RIGHT_PARENTHESIS		= new Token(")");// 7
	public static final Token LEFT_PARENTHESIS 		= new Token("(");// 8
	public static final Token PERIOD 				= new Token(".");// 9
	public static final Token GREATERTHAN_SYMBOL	= new Token(">");//10
	public static final Token LESSTHAN_SYMBOL		= new Token("<");//11
	public static final Token GREATEROREQUAL_SYMBOL	= new Token(">=");//12
	public static final Token LESSOREQUAL_SYMBOL	= new Token("<=");//13
	public static final Token EQUAL_SYMBOL			= new Token("=");//14
	public static final Token INEQUAL_SYMBOL		= new Token("#");//15
	public static final Token BEGIN_SYMBOL 			= new Token("begin");// 16
	public static final Token END_SYMBOL 			= new Token("end");// 17
	public static final Token READ_SYMBOL 			= new Token("read");// 18
	public static final Token WRITE_SYMBOL 			= new Token("write");// 19
	public static final Token EOF_SYMBOL 			= new Token("eof");// 20
	public static final Token MULTIPLY_OPERATOR		= new Token("*");//21
	public static final Token DIVIDE_OPERATOR		= new Token("/");//22
	public static final Token SKIP_SYMBOL			= new Token("skip");//23
	public static final Token IF_SYMBOL				= new Token("if");//24
	public static final Token THEN_SYMBOL			= new Token("->");//25
	public static final Token ELSE_SYMBOL			= new Token("[]");//26
	public static final Token ENDIF_SYMBOL			= new Token("fi");//27
	// Add new tokens here please and number them in comments
	
	public String name() {
		return name;
	}// Only used in error reporting.

	public static String spelling() {
		return Token.buffer;
	}

	static void bufferChar(final char character) {
		buffer += character;
	}

	static void clearBuffer() {
		buffer = "";
	}

	private static String buffer = "";
	private final String name;

	private Token(final String name) {
		this.name = name;
	}
}

// ------------------------------ Scanner -------------------------------
class Scanner {
	public Scanner(final BufferedReader in, final PrintWriter out) {
		this.in = in;
		this.out = out;
	}

	public Token currentToken() {
		if (!tokenAvailable) {
			savedToken = getNextToken();
			tokenAvailable = true;
		}
		return savedToken;
	}

	public void match(final Token token) {
		if (token != currentToken()) {
			error(token);
		} else {
			tokenAvailable = false;
		}
	}

	private final BufferedReader in;
	private final PrintWriter out;
	private String lineBuffer;
	private int lineLength = 0;
	private int linePointer = 0;
	private boolean EOF = false;
	private boolean tokenAvailable = false;
	private Token savedToken = Token.EOF_SYMBOL;
	private int errors = 0;

	private void getNewLine() {
		lineBuffer = "";
		try {
			lineBuffer = in.readLine();
			if (lineBuffer == null) {
				lineBuffer = " ";
				EOF = true;
			}
		} catch (IOException e) {
			EOF = true;
			lineBuffer = " ";
		}
		lineBuffer += ' ';
		lineLength = lineBuffer.length();
		out.println("    %" + lineBuffer);
		linePointer = 0;
	}

	private char inspect() {
		if (linePointer >= lineLength) {
			getNewLine();
		}
		return lineBuffer.charAt(linePointer);
	}

	private void advance() {
		linePointer++;
	}

	private char getNextChar() {
		char character = inspect();
		advance();
		return character;
	}

	private Token checkReserved() {
		if (Token.spelling().equalsIgnoreCase(Token.BEGIN_SYMBOL.name())) {
			return Token.BEGIN_SYMBOL;
		}
		if (Token.spelling().equalsIgnoreCase(Token.END_SYMBOL.name())) {
			return Token.END_SYMBOL;
		}
		if (Token.spelling().equalsIgnoreCase(Token.READ_SYMBOL.name())) {
			return Token.READ_SYMBOL;
		}
		if (Token.spelling().equalsIgnoreCase(Token.WRITE_SYMBOL.name())) {
			return Token.WRITE_SYMBOL;
		}
		if (Token.spelling().equalsIgnoreCase(Token.IF_SYMBOL.name())) {
			return Token.IF_SYMBOL;
		}
		if (Token.spelling().equalsIgnoreCase(Token.ENDIF_SYMBOL.name())) {
			return Token.ENDIF_SYMBOL;
		}
		if (Token.spelling().equalsIgnoreCase(Token.SKIP_SYMBOL.name())) {
			return Token.SKIP_SYMBOL;
		}
		return Token.IDENTIFIER;
	}

	private void lexicalError(final char character) {
		out.println("Lexical Error Detected at '" + character + "'");
		errors++;
	}

	private void error(final Token token) {
		out.println("Match Error Detected Expecting: " + token.name() + " saw: " + savedToken.name());
		errors++;
		System.exit(1);
	}

	static final char blank = ' ';
	static final char tab = '	';

	private Token getNextToken() {
		Token result = Token.EOF_SYMBOL;
		if (EOF) {
			return Token.EOF_SYMBOL;
		}
		Token.clearBuffer();
		boolean finished = false;
		while (!EOF && !finished) {
			char currentChar = getNextChar();
			switch (currentChar) {
			case blank:
			case tab:
				break;
			case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G':
			case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N':
			case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U':
			case 'V': case 'W': case 'X': case 'Y': case 'Z':
			case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g':
			case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n':
			case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u':
			case 'v': case 'w': case 'x': case 'y': case 'z': {
				Token.bufferChar(currentChar);
				while (!finished) {
					switch (inspect()) {
					case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G':
					case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N':
					case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U':
					case 'V': case 'W': case 'X': case 'Y': case 'Z':
					case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g':
					case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n':
					case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u':
					case 'v': case 'w': case 'x': case 'y': case 'z': 
					case '0': case '1': case '2': case '3': case '4': 
					case '5': case '6': case '7': case '8': case '9':
					case '_': {
						Token.bufferChar(inspect());
						advance();
					}
						break;
					default: {
						result = checkReserved();
						finished = true;
					}
						break;
					}
				}
			}
				break;
			case '0': case '1': case '2': case '3': case '4': 
			case '5': case '6': case '7': case '8': case '9': {
				Token.bufferChar(currentChar);
				while (!finished) {
					switch (inspect()) {
					case '0': case '1': case '2': case '3': case '4': 
					case '5': case '6': case '7': case '8': case '9':{
						Token.bufferChar(inspect());
						advance();
					}
						break;
					default: {
						result = Token.INTEGER_LITERAL;
						finished = true;
					}
						break;
					}
				}
			}
				break;
			case '(': {
				result = Token.LEFT_PARENTHESIS;
				finished = true;
			}
				break;
			case ')': {
				result = Token.RIGHT_PARENTHESIS;
				finished = true;
			}
				break;
			case ';': {
				result = Token.SEMICOLON;
				finished = true;
			}
				break;
			case ',': {
				result = Token.COMMA;
				finished = true;
			}
				break;
			case '+': {
				result = Token.PLUS_OPERATOR;
				finished = true;
			}
				break;
			case '*': {
				result = Token.MULTIPLY_OPERATOR;
				finished = true;
			}
				break;
			case '/': {
				result = Token.DIVIDE_OPERATOR;
				finished = true;
			}
			case '>': {
				if (inspect() == '=') {
					advance();
					result = Token.GREATEROREQUAL_SYMBOL;
					finished = true;
				} else {
					result = Token.GREATERTHAN_SYMBOL;
					finished = true;
				}
			}
			case '<': {
				if (inspect() == '=') {
					advance();
					result = Token.LESSOREQUAL_SYMBOL;
					finished = true;
				} else {
					result = Token.LESSTHAN_SYMBOL;
					finished = true;
				}
			}
			case '=': {
				result = Token.EQUAL_SYMBOL;
				finished = true;
			}
			case '#': {
				result = Token.INEQUAL_SYMBOL;
				finished = true;
			}
				break;
			case ':': {
				if (inspect() == '=') {
					advance();
					result = Token.ASSIGN_OPERATOR;
					finished = true;
				} else {
					lexicalError(inspect());
				}
			}
				break;
			case '[': {
				if (inspect() == ']') {
					advance();
					result = Token.ELSE_SYMBOL;
					finished = true;
				} else {
					lexicalError(inspect());
				}
			}
				break;
			case '.': {
				result = Token.PERIOD;
				finished = true;
			}
				break;
			case '-': {
				if (inspect() == '-') {
					getNewLine();
				} else {
					result = Token.MINUS_OPERATOR;
					finished = true;
				}
			}
				break;
			default: {
				lexicalError(currentChar);
			}
				break;
			}
		}
		if (!finished) {
			result = Token.EOF_SYMBOL;
		}
		return (result);
	}

	public int errors() {
		return errors;
	}
}

// ------------------------------ Parser -------------------------------
class Parser {
	public Parser(final PrintWriter out, final Scanner scanner, final SymbolTable symbolTable, final SemanticActions semantics) {
		this.out = out;
		this.scanner = scanner;
		this.symbolTable = symbolTable;
		this.semantics = semantics;
	}

	public void systemGoal() { // <system goal> -> #Start <program> "EOF_SYMBOL" #Finish
		semantics.start();
		program();
		scanner.match(Token.EOF_SYMBOL);
		semantics.finish();
	}

	private void syntaxError(final Token token) {
		out.println("Syntax Error detected. Token was: " + token.name());
		errors++;
	}

	private void program() { // <program> -> "BEGIN" <statement list> "END" "."
		scanner.match(Token.BEGIN_SYMBOL);
		statementList();
		scanner.match(Token.END_SYMBOL);
		scanner.match(Token.PERIOD);
	}

	private void statementList() { // <statement list> -> <statement> {<statement>}
		statement();
		while (scanner.currentToken() == Token.IDENTIFIER
				|| scanner.currentToken() == Token.READ_SYMBOL
				|| scanner.currentToken() == Token.WRITE_SYMBOL
				|| scanner.currentToken() == Token.IF_SYMBOL
				|| scanner.currentToken() == Token.SKIP_SYMBOL) {
			statement();
		}
	}

	private void statement() {
	// <statement> -> <variable> ":=" <expression> #Assign ";"
	// <statement> -> "READ" <var list> ";"
	// <statement> -> "WRITE" <expr list> ";" #EndWrite
	// <statement> -> "IF" <guard> #ifTest "->" <statement list> #endIfPart "[]" #startElsePart <statement list> #endIf"FI "";"
	// <statement> -> "SKIP"";"
		Expression identifier;
		Expression expression;
		if (scanner.currentToken() == Token.IDENTIFIER) {
			identifier = variable();
			scanner.match(Token.ASSIGN_OPERATOR);
			expression = expression();
			semantics.assign(identifier, expression);
			scanner.match(Token.SEMICOLON);
		} else if (scanner.currentToken() == Token.READ_SYMBOL) {
			scanner.match(Token.READ_SYMBOL);
			variableList();
			scanner.match(Token.SEMICOLON);
		} else if (scanner.currentToken() == Token.WRITE_SYMBOL) {
			scanner.match(Token.WRITE_SYMBOL);
			expressionList();
			scanner.match(Token.SEMICOLON);
			semantics.endWrite();
		} else if (scanner.currentToken() == Token.IF_SYMBOL) {
			scanner.match(Token.IF_SYMBOL);
			LabelExpression jumpToElse = new LabelExpression(nextLabelNumber());
			semantics.ifTest(guard(), jumpToElse);
			scanner.match(Token.THEN_SYMBOL);
			statementList();
			LabelExpression jumpToEnd = new LabelExpression(nextLabelNumber());
			semantics.endIfPart(jumpToEnd);
			scanner.match(Token.ELSE_SYMBOL);
			semantics.beginElsePart(jumpToElse);
			statementList();
			semantics.endIf(jumpToEnd);
			scanner.match(Token.ENDIF_SYMBOL);
			scanner.match(Token.SEMICOLON);
		}else if (scanner.currentToken() == Token.SKIP_SYMBOL) {
			scanner.match(Token.SKIP_SYMBOL);
			scanner.match(Token.SEMICOLON);
		}	else {
			syntaxError(scanner.currentToken());
		}
	}

	private void variableList() { 
		// <var list> -> <variable> #ReadVar {"," <variable> #ReadVar }
		Expression variable = variable();
		semantics.readVariable(variable);
		while (scanner.currentToken() == Token.COMMA) {
			scanner.match(Token.COMMA);
			variable = variable();
			semantics.readVariable(variable);
		}
	}

	private void expressionList() { 
		// <expr list> -> <expression> #WriteExpr {"," <expression> #WriteExpr}
		Expression expression = expression();
		semantics.writeExpression(expression);
		while (scanner.currentToken() == Token.COMMA) {
			scanner.match(Token.COMMA);
			expression = expression();
			semantics.writeExpression(expression);
		}
	}
	
	private RelativeOperator guard(){
		//<guard> -> <expression> <rel op> <expression>
		Expression rightCondition;
		RelativeOperator relationOp = relativeOperator(scanner.currentToken());
		Expression result = expression();
		if (scanner.currentToken() == Token.GREATERTHAN_SYMBOL 	|| scanner.currentToken() == Token.GREATEROREQUAL_SYMBOL || 
				scanner.currentToken() == Token.LESSTHAN_SYMBOL || scanner.currentToken() == Token.LESSOREQUAL_SYMBOL || 
				scanner.currentToken() == Token.EQUAL_SYMBOL 	|| scanner.currentToken() == Token.INEQUAL_SYMBOL){
			relationOp = relativeOperator(scanner.currentToken());
			rightCondition = expression();
			result = semantics.relativeExpression(result, relationOp, rightCondition);
		}
		else{
			syntaxError(scanner.currentToken());
		}
		return relationOp;
	}

	private Expression expression() { 
		// <expression> -> <factor> {<multiply op> <factor> #AddExpression}
		Expression rightOperand;
		AddOperator operator;
		Expression result = factor();
		while (scanner.currentToken() == Token.PLUS_OPERATOR || scanner.currentToken() == Token.MINUS_OPERATOR) {
			operator = addOperator();
			rightOperand = factor();
			result = semantics.addExpression(result, operator, rightOperand);
		}
		return result;
	}
	
	private Expression factor() { 
		// <factor> -> <primary> {<add op> <primary> #AddExpression}
		Expression rightOperand;
		MultiplyOperator operator;
		Expression result = primary();
		while (scanner.currentToken() == Token.MULTIPLY_OPERATOR || scanner.currentToken() == Token.DIVIDE_OPERATOR) {
			operator = multiplyOperator();
			rightOperand = primary();
			result = semantics.multiplyExpression(result, operator, rightOperand);
		}
		return result;
	}

	private Expression primary() {
	// <primary> -> "(" <expression> ")" | <variable> | "INTEGER_LITERAL"
		Expression result = null;
		if (scanner.currentToken() == Token.LEFT_PARENTHESIS) {
			scanner.match(Token.LEFT_PARENTHESIS);
			result = expression();
			scanner.match(Token.RIGHT_PARENTHESIS);
		} else if (scanner.currentToken() == Token.IDENTIFIER) {
			result = variable();
		} else if (scanner.currentToken() == Token.INTEGER_LITERAL) {
			result = new LiteralExpression(Token.spelling());
			scanner.match(Token.INTEGER_LITERAL);
		} else {
			syntaxError(scanner.currentToken());
		}
		return result;
	}

	private AddOperator addOperator() { // <add op> -> "+" | "-"
		Token token = scanner.currentToken();
		AddOperator result = null;
		if (token == Token.PLUS_OPERATOR) {
			result = AddOperator.PLUS;
			scanner.match(token);
		} else if (token == Token.MINUS_OPERATOR) {
			result = AddOperator.MINUS;
			scanner.match(token);
		} else {
			syntaxError(token);
		}
		return result;
	}
	
	private MultiplyOperator multiplyOperator() { // <mult op> -> "*" | "/"
		Token token = scanner.currentToken();
		MultiplyOperator result = null;
		if (token == Token.MULTIPLY_OPERATOR) {
			result = MultiplyOperator.TIMES;
			scanner.match(token);
		} else if (token == Token.DIVIDE_OPERATOR) {
			result = MultiplyOperator.DIVIDE;
			scanner.match(token);
		} else {
			syntaxError(token);
		}
		return result;
	}
	
	private RelativeOperator relativeOperator(Token token) { // <rel op> -> ">" | ">=" | "<" | "<=" | "=" | "#"
		RelativeOperator result = null;
		if (token == Token.GREATERTHAN_SYMBOL){
			result = RelativeOperator.GREATERTHAN;
			scanner.match(token);
		}
		else if (token == Token.GREATEROREQUAL_SYMBOL){
			result = RelativeOperator.GREATERTHANOREQUAL;
			scanner.match(token);
		}
		else if (token == Token.LESSTHAN_SYMBOL){
			result = RelativeOperator.LESSTHAN;
			scanner.match(token);
		}
		else if (token == Token.LESSOREQUAL_SYMBOL){
			result = RelativeOperator.LESSTHANOREQUAL;
			scanner.match(token);
		}
		else if (token == Token.EQUAL_SYMBOL){
			result = RelativeOperator.EQUAL;
			scanner.match(token);
		}
		else if (token == Token.INEQUAL_SYMBOL) {
			result = RelativeOperator.INEQUAL;
			scanner.match(token);
		} else {
			syntaxError(token);
		}
		return result;
	}

	private Expression variable() { // <variable> -> "IDENTIFIER" #ProcessIdentifier
		scanner.currentToken(); // update the spelling
		Expression result = semantics.processIdentifier(new IdExpression(Token.spelling()));
		scanner.match(Token.IDENTIFIER);
		return result;
	}
	
	public int nextLabelNumber(){
		labelNumber++;
		return labelNumber;
	}

	public int errors() {
		return errors;
	}

	private final PrintWriter out;
	private final Scanner scanner;
	private final SymbolTable symbolTable;
	private final SemanticActions semantics;
	private int labelNumber = 0;
	private int errors = 0;
}

// ------------------------------ SemanticActions ------------------------
// ------------------------- First the Semantic Records --------------------
// --- These are the params and return values of parser and semantic methods
// ------------------------------ AddOperator ----------------------------
class AddOperator { // Typed enumeration.
	private AddOperator(final String samCode) {
		code = samCode;
	}

	public static final AddOperator PLUS = new AddOperator("IA");
	public static final AddOperator MINUS = new AddOperator("IS");

	public String samCode() {
		return code;
	}

	private final String code;
}

class MultiplyOperator { // Typed enumeration.
	private MultiplyOperator(final String samCode) {
		code = samCode;
	}

	public static final MultiplyOperator TIMES = new MultiplyOperator("IM");
	public static final MultiplyOperator DIVIDE = new MultiplyOperator("ID");

	public String samCode() {
		return code;
	}

	private final String code;
}
class RelativeOperator { // Typed enumeration.
	private RelativeOperator(final String code, final String relOp) {
		this.code = code;
		this.jumpCode = relOp;
	}
	
	public static final RelativeOperator GREATERTHAN = new RelativeOperator("IC","JLE");
	public static final RelativeOperator GREATERTHANOREQUAL = new RelativeOperator("IC","JLT");
	public static final RelativeOperator LESSTHAN = new RelativeOperator("IC","JGE");
	public static final RelativeOperator LESSTHANOREQUAL = new RelativeOperator("IC","JGT");
	public static final RelativeOperator EQUAL = new RelativeOperator("IC","JNE");
	public static final RelativeOperator INEQUAL = new RelativeOperator("IC","JEQ");
	
	public String samCode() {
		return code;
	}
	
	public String jumpCode(){
		return jumpCode;
	}

	private final String code;
	private final String jumpCode;
}

// ------------------------------ Expression --------------------------
interface Expression {
	public abstract String samCode();
}

class IdExpression implements Expression { // Represents an identifier: Immutable
	public IdExpression(final String name) {
		this.name = name;
	}

	public String samCode() {
		return "$" + name + "$";
	}

	private final String name; // The spelling of the identifier
}

class LiteralExpression implements Expression { // Represents a numeric literal: Immutable
	public LiteralExpression(final String value) {
		this.value = Integer.parseInt(value);
	}

	public String samCode() {
		return "#" + value;
	};

	private final int value;// The literal value.
}

class TemporaryExpression implements Expression { // Represents a cpu register: Immutable
	public TemporaryExpression(final int which) {
		this.which = which;
	}

	public String samCode() {
		return "R" + which;
	}

	public int which() {
		return which;
	}

	private final int which; // The register number
}

class LabelExpression implements Expression { // Represents a label title: Immutable
	public LabelExpression(final int newId) {
		this.id = new String(""+newId);
	}

	public String samCode() {
		return id;
	}

	private final String id; // The spelling of the label's title
}

// ------------------------------ SemanticActions Class ------------------
class SemanticActions {
	public SemanticActions(final PrintWriter out, final CodeGenerator codegen,
			SymbolTable idtable) {
		this.out = out;
		this.codegenerator = codegen;
		this.symbolTable = idtable;
	}

	void semanticError(final String message) {
		out.println("SemanticActions Error: " + message);
		errors++;
		System.exit(1);
	}

	public void start() {
		out.println("    %  Compiled on " + new Date());
		out.println("    %  Author(s): Joseph Bergin, Drew Cimino");
	}

	public void finish() {
		codegenerator.generate0Address("HALT");
		codegenerator.generateVariables();
	}

	public Expression processIdentifier(final IdExpression spelling) {
		symbolTable.checkIdentifier(spelling.samCode());
		return spelling;
	}

	public Expression addExpression(final Expression left, final AddOperator addOp, final Expression right) {
		TemporaryExpression register = codegenerator.loadRegister(left);
		codegenerator.generate2Address(addOp.samCode(), register, right);
		codegenerator.freeTemporary(right);
		return register;
	}
	
	public Expression multiplyExpression(final Expression left, final MultiplyOperator multiplyOp, final Expression right) {
		TemporaryExpression register = codegenerator.loadRegister(left);
		codegenerator.generate2Address(multiplyOp.samCode(), register, right);
		codegenerator.freeTemporary(right);
		return register;
	}
	
	public Expression relativeExpression(final Expression left, final RelativeOperator relativeOp, final Expression right){
		TemporaryExpression register = codegenerator.loadRegister(left);
		codegenerator.generate2Address(relativeOp.samCode(), register, right);
		codegenerator.freeTemporary(right);
		return register;  
	}
	
	public void ifTest(RelativeOperator relOp, LabelExpression target){
		codegenerator.generate1Address(relOp.jumpCode(), target);		
	}
	
	public void endIfPart(LabelExpression target){
		codegenerator.generate1Address("JMP", target);
	}
	
	public void beginElsePart(LabelExpression target){
		codegenerator.generate1Address("LABEL", target);
	}
	
	public void endIf(LabelExpression target){
		codegenerator.generate1Address("LABEL", target);
	}

	public void assign(final Expression target, final Expression source) {
		TemporaryExpression register = codegenerator.loadRegister(source);
		codegenerator.generate2Address("STO", register, target);
		codegenerator.freeTemporary(register);
		codegenerator.freeTemporary(source);
		codegenerator.freeTemporary(target);
	}

	public void writeExpression(final Expression outExpression) {
		codegenerator.generate1Address("WRI", outExpression);
		codegenerator.freeTemporary(outExpression);
	}

	public void endWrite() {
		codegenerator.generate0Address("WRNL");
	}

	public void readVariable(final Expression inVariable) {
		codegenerator.generate1Address("RDI", inVariable);
	}

	public int errors() {
		return errors;
	}

	private final PrintWriter out;
	private final CodeGenerator codegenerator;
	private final SymbolTable symbolTable;
	private int errors = 0;
}

// ------- CodeGenerator -- See the SAM documentation ---------------------
class CodeGenerator {
	public CodeGenerator(final PrintWriter out, final SymbolTable symbolTable) {
		this.out = out;
		this.symbolTable = symbolTable;
		for (int i = 0; i < TOTAL_REGISTERS; ++i) {
			freeRegisters[i] = true;
		}
	}

	public TemporaryExpression getTemporary() { // There are exactly 16 registers.
		int register = 0;
		while (register < TOTAL_REGISTERS && !freeRegisters[register]) {
			register++;
		}
		if (register < TOTAL_REGISTERS) {
			freeRegisters[register] = false;
		}
		return new TemporaryExpression(register);
	}

	public TemporaryExpression loadRegister(final Expression expression) {
		if (expression instanceof TemporaryExpression) {
			return (TemporaryExpression) expression;
		}
		TemporaryExpression register = getTemporary();
		generate2Address("LD", register, expression);
		return register;
	}

	public void registerReport() {
		out.print("  --    Allocated registers:");
		System.out.print("  --    Allocated registers:");
		String finalMessage = " None";
		int register = 0;
		for(boolean free: freeRegisters){
			if(!free){
				out.print(" " + register);
				System.out.print(" " + register);
				finalMessage = " <- Find and fix.";				
			}
			register++;
		}
		out.println(finalMessage);
		System.out.println(finalMessage);
	}

	public void freeTemporary(final Expression expression) {
		if (expression instanceof TemporaryExpression) {
			freeRegisters[((TemporaryExpression) expression).which()] = true;
		}
	}

	public void generate0Address(final String opcode) {
		out.println(BLANKS + opcode);
	}// Generate a 0 address SAM instruction like halt

	public void generate1Address(final String opcode, final Expression arg) {
		out.println(BLANKS + pad(opcode) + arg.samCode());
	} // generate 1 address SAM instruction like rdi

	public void generate2Address(final String opcode, final TemporaryExpression arg1, final Expression arg2) {
		out.println(BLANKS + pad(opcode) + arg1.samCode() + ",  " + arg2.samCode());
	}// generate a 2 address SAM instruction like ia

	public void generateVariables() { // generate code for the variable block at the end.
		for(String variable: symbolTable){
			generateLabel(variable);
			out.println(BLANKS + pad("SKIP") + INTEGER_SIZE);
		}
	}
	
	private String pad(final String value){
		int opCodePadLength = 8;
		String result = value;
		for(int i = result.length(); i < opCodePadLength; ++i){
			result += " ";
		}
		return result;
	}
	
	public void generateLabel(final String labelValue){
		out.println("LABEL    " + labelValue);
	}

	private final PrintWriter out;
	private static final int TOTAL_REGISTERS = 16;
	private static final int INTEGER_SIZE = 2;
	private final boolean freeRegisters[] = new boolean[TOTAL_REGISTERS];
	private final SymbolTable symbolTable;
	private static final String BLANKS = "    ";//Left padding of instructions
}

// ------------------------------ SymbolTable -------------------------------

class SymbolTable implements Iterable<String>{ // Too simple to be realistic, actually.
	public SymbolTable(PrintWriter messages) {
		out = messages;
	}

	public int lookUp(final String symbol) {
		return symbolTable.indexOf(symbol);
	}

	private int enter(final String symbol) {
		symbolTable.add(symbol);
		int result = symbolTable.size() - 1;
		return result;
	}

	public void checkIdentifier(final String symbol) {
		int where = lookUp(symbol);
		if (where < 0) {
			where = enter(symbol);
			out.println("    %    Implicit declaration of: "
					+ symbol.substring(1, symbol.length() - 1));
		}
	}

	public Iterator<String> iterator() {
		return symbolTable.iterator();
	}

	private final List<String> symbolTable = new ArrayList<String>();
	private final PrintWriter out;
}

// --------------------------- MicroGCLCompiler ---------------------------
public class MicroGCLCompiler {
	public static void main(String[] args) {
		if (args.length < 2) {
			BufferedReader inp = new BufferedReader(new InputStreamReader( System.in));
			String[] temp = new String[2];
			if (args.length < 1) {
				System.out.println("Enter the input fliename");
				try {
					temp[0] = inp.readLine();
				} catch (IOException ex) {
					System.out.println("Error reading input filename.");
				}
			} else {
				temp[0] = args[0];
			}
			System.out.println("Enter the listing filename [codefile]");
			try {
				temp[1] = "codefile";
				String in = inp.readLine();
				if (in != null && !in.equals("")) {
					temp[1] = in;
				}
			} catch (IOException ex) {
				System.out.println("Error reading listing filename.");
			}
			args = temp;
		}
		try {
			BufferedReader sourceFile = new BufferedReader(new FileReader( args[0]));
			PrintWriter listingFile = new PrintWriter(new FileWriter(args[1]), true);
			Scanner scanner = new Scanner(sourceFile, listingFile);
			SymbolTable idtable = new SymbolTable(listingFile);
			CodeGenerator codegen = new CodeGenerator(listingFile, idtable);
			SemanticActions semantic = new SemanticActions(listingFile, codegen, idtable);
			Parser parser = new Parser(listingFile, scanner, idtable, semantic);
			parser.systemGoal();
			listingFile.print("     %    End of Compilation");
			codegen.registerReport();
			int totalErrors = scanner.errors() + parser.errors() + semantic.errors();
			String errorMessage;
			switch (totalErrors) {
			default: {
				errorMessage = "were " + totalErrors + " errors.";
			}
				break;
			case 0: {
				errorMessage = "were no errors.";
			}
				break;
			case 1: {
				errorMessage = "was 1 error.";
			}
				break;
			}
			System.out.println("Done. There " + errorMessage);
			listingFile.println("     %    There " + errorMessage);
		} catch (IOException e) {
			System.out.println("File errors: " + e);
			System.exit(1);
		}
	}
}