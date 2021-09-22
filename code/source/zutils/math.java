package zutils;

// -----( IS Java Code Template v1.2

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
import java.util.HashMap;
import java.util.Map;
// --- <<IS-END-IMPORTS>> ---

public final class math

{
	// ---( internal utility methods )---

	final static math _instance = new math();

	static math _newInstance() { return new math(); }

	static math _cast(Object o) { return (math)o; }

	// ---( server methods )---




	public static final void calculate (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(calculate)>> ---
		// @sigtype java 3.5
		// [i] field:0:required expression
		// [i] record:0:required vars
		// [i] - field:0:required a
		// [i] - field:0:required b
		// [o] field:0:required result
		IDataCursor pipelineCursor = pipeline.getCursor();
		
		String expression = IDataUtil.getString( pipelineCursor, "expression" );
		if (expression == null || expression.length() == 0) throw new ServiceException("ZUtils::math::calc Exception :: Expression is null or empty!");
		
		try {
			CompactCalc cc = new CompactCalc(expression);
			IData vars = IDataUtil.getIData( pipelineCursor, "vars" );
			if (vars != null) {
				IDataCursor varsCursor = vars.getCursor();
				while (varsCursor.next()) {
					cc.setVar(varsCursor.getKey(), new Double(""+varsCursor.getValue()).doubleValue());
				}
				varsCursor.destroy();
			}
			IDataUtil.put( pipelineCursor, "result", new Double(cc.execute()).toString() );
		} catch (Exception e) {
			throw new ServiceException("ZUtils::math::calc Exception :: " + e.getMessage());
		}
		
		pipelineCursor.destroy();	
		// --- <<IS-END>> ---

                
	}

	// --- <<IS-START-SHARED>> ---
	}
	
	/*
	 * Mathematical expression parser/calculator - all in one class
	 * with implementation of recursive descent parser method.
	 *
	 * @author Ed Aponasko
	 * @version 1.0, June 2021
	 */
	class CompactCalc {
	
	private static final char EOF = '\0';
	
	// List of pre-defined constants like pi, e... etc.
	private static Map<String, Double> CONSTANTS;
	static {
	    CONSTANTS = new HashMap<>();
	    CONSTANTS.put("PI", Math.PI);
	    CONSTANTS.put("E", Math.E);
	}
	
	// expression
	private final String code;
	
	// length of expression
	private final int length;
	
	// our current position within the expression
	private int pos;
	
	public CompactCalc(String code) {
	    this.code = code;
	    this.length = code.length();
	}
	
	public void setVar(String name, Double value) {
	    CONSTANTS.put(name, value);
	}
	
	public double execute() throws Exception {
	    if (code.isEmpty()) throw new Exception("Error :: Empty expression!");
	    double result = expression();
	    if (peek() != EOF)
	        throw new Exception("Error :: Cannot parse following code : " + code.substring(pos));
	    return result;
	}
	
	public String getExpression() {
	    return code;
	}
	
	/*
	    Wrapper for calculated expression
	 */
	private double expression() {
	    return addition();
	}
	
	/*
	    Method to handle addition and subtraction
	 */
	private double addition() {
	    double result = multiplication();
	    while (true) {
	        char next = peek();
	        switch(next) {
	            case '+' :
	                getNext();
	                result = result + multiplication();
	                continue;
	            case '-' :
	                getNext();
	                result = result - multiplication();
	                continue;
	        }
	        break;
	    }
	    return result;
	}
	
	/*
	    Method to handle multiplication, division, modulus and bitwise operations
	    & - bitwise AND
	        A bitwise AND is a binary operation that takes two equal-length binary representations
	        and performs the logical AND operation on each pair of the corresponding bits,
	        which is equivalent to multiplying them.
	                0101 (decimal 5)
	            AND 0011 (decimal 3)
	              = 0001 (decimal 1)
	    | - bitwise OR
	        A bitwise OR is a binary operation that takes two bit patterns of equal length and
	        performs the logical inclusive OR operation on each pair of corresponding bits.
	                0101 (decimal 5)
	             OR 0011 (decimal 3)
	              = 0111 (decimal 7)
	    ~ - bitwise NOT
	        The bitwise NOT, or complement, is a unary operation that performs logical negation
	        on each bit, forming the ones' complement of the given binary value.
	        Bits that are 0 become 1, and those that are 1 become 0.
	            NOT 0111  (decimal 7)
	              = 1000  (decimal 8)
	 */
	private double multiplication() {
	    double result = exponentiation();
	    while (true) {
	        char next = peek();
	        switch(next) {
	            case '*' :
	                getNext();
	                result = result * exponentiation();
	                continue;
	            case '/' :
	                getNext();
	                result = result / exponentiation();
	                continue;
	            case '%' :
	                getNext();
	                result = result % exponentiation();
	                continue;
	            case '~' :
	                getNext();
	                result = ~ (int) expression();
	                continue;
	            case '&' :
	                getNext();
	                result = (int) result & (int) expression();
	                continue;
	            case '|' :
	                getNext();
	                result = (int) result | (int) expression();
	                continue;
	        }
	        break;
	    }
	    return result;
	}
	
	/*
	    Method to care about exponents
	 */
	private double exponentiation() {
	    double result = parenthesis();
	    while (true) {
	        char next = peek();
	        switch(next) {
	            case '^' :
	                getNext();
	                result = Math.pow(result,  parenthesis());
	                continue;
	            case '!' :
	                getNext();
	                long fact = 1;
	                for (int i = 2; i <= result; i++) {
	                    fact = fact * i;
	                }
	                result = fact;
	                continue;
	        }
	        break;
	    }
	    return result;
	}
	
	/*
	    Method to handle opening and closing parenthesis in expression
	 */
	private double parenthesis() {
	    char current = peek();
	    if (current == '(') {
	        getNext(); // skipping (
	        double result = expression();
	        current = peek();
	        if (current != ')') throw new RuntimeException("Error :: Expected closing parenthesis");
	        getNext(); // skipping )
	        return result;
	    }
	    return primary();
	}
	
	/*
	    Primary method to handle Numbers, Float numbers, unary operator, constants and pre-defined functions
	 */
	private double primary() {
	    final char current = peek();
	    if (current == '-') {
	        getNext();
	        return -tokenizeNumber();
	    }
	    if (Character.isDigit(current)) {
	        return tokenizeNumber();
	    }
	    if (Character.isLetter(current)) {
	        return tokenizeFuncOrConstant();
	    }
	    return 0;
	}
	
	/*
	    Helper to take care of all constants and functions
	 */
	private double tokenizeFuncOrConstant() {
	    final StringBuilder buffer = new StringBuilder();
	    char current = peek();
	    while (true) {
	        if (!Character.isLetterOrDigit(current) && (current != '_') && (current != '$') ) {
	            break;
	        }
	        buffer.append(current);
	        current = getNext();
	    }
	    final String word = buffer.toString();
	    // check for constants
	    if (CONSTANTS.containsKey(word)) {
	        return CONSTANTS.get(word);
	    }
	    // constant not found, check for function
	    Double result = null;
	    if (current == '(') {
	        getNext(); // skipping (
	        switch (word) {
	            // sine function
	            case "sin" : result = Math.sin(expression()); break;
	            // hyperbolic sine function
	            case "sinh": result = Math.sinh(expression()); break;
	            // cosine function
	            case "cos": result = Math.cos(expression()); break;
	            // tangent function
	            case "tan": result = Math.tan(expression()); break;
	            // cotangent function
	            case "ctg": result = 1 / Math.tan(expression()); break;
	            // absolute value of a number
	            case "abs": result = Math.abs(expression()); break;
	            // logarithm of a given number x
	            case "ln": result = Math.log(expression()); break;
	            // base 10 logarithm of a number
	            case "lg": result = Math.log10(expression()); break;
	            // square root
	            case "sqrt": result = Math.sqrt(expression()); break;
	            // to radians
	            case "toRadians": result = Math.toRadians(expression()); break;
	            // to degrees
	            case "toDegrees": result = Math.toDegrees(expression()); break;
	        }
	        if (result == null) throw new RuntimeException("Error :: Cannot recognize function '" + word + "'");
	        current = peek();
	        if (current != ')') throw new RuntimeException("Error :: Expected closing parenthesis");
	        getNext(); // skipping )
	        return result;
	    } else {
	        throw new RuntimeException("Error :: Cannot recognize constant '" + word + "'");
	    }
	}
	
	/*
	    Numbers and floats parser
	 */
	private double tokenizeNumber() {
	    final StringBuilder sb = new StringBuilder();
	    char current = peek();
	    while (true) {
	        // float number support
	        if (current == '.') {
	            if (sb.indexOf(".") != -1) throw new RuntimeException("Error :: Invalid float number!");
	        } else if (!Character.isDigit(current)) {
	            break;
	        }
	        sb.append(current);
	        current = getNext();
	    }
	    return new Double(sb.toString());
	}
	
	/*
	    Method to return next char from the provided expression
	 */
	private char getNext() {
	    pos++;
	    return peek();
	}
	
	/*
	    Method to peek the next char in the provided expression
	 */
	private char peek() {
	    final int position = pos;
	    if (position >= length) return EOF;
	    final char current = code.charAt(position);
	    if (" \n\r\t".indexOf(current) > -1) {
	        // skip whitespaces
	        pos++;
	        return peek();
	    }
	    return current;
	}
		
	// --- <<IS-END-SHARED>> ---
}

