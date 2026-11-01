
package com.example.sovereign_ai.Tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class CalculatorTool {

    private static final MathContext MC =
            new MathContext(16, RoundingMode.HALF_UP);

    @Tool(description = """
            Perform arithmetic calculations accurately.
            Supports addition, subtraction, multiplication,
            division and percentages.
            Use this tool whenever the user asks for calculations.
            """)
    public String calculate(
            @ToolParam(description = "First number") double a,
            @ToolParam(description =
                    "Operation: add, subtract, multiply, divide, percentage")
            String operation,
            @ToolParam(description = "Second number") double b
    ) {
        try {
            if (!Double.isFinite(a) || !Double.isFinite(b)) {
                return "Error: numbers must be finite.";
            }

            BigDecimal x = BigDecimal.valueOf(a);
            BigDecimal y = BigDecimal.valueOf(b);

            BigDecimal result = switch (
                    operation.trim().toLowerCase()
                    ) {
                case "add", "+" -> x.add(y);
                case "subtract", "-" -> x.subtract(y);
                case "multiply", "*" -> x.multiply(y);
                case "divide", "/" -> {
                    if (y.signum() == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    yield x.divide(y, MC);
                }
                case "percentage", "%" ->
                        x.multiply(y).divide(
                                BigDecimal.valueOf(100), MC
                        );
                default -> throw new IllegalArgumentException(
                        "Unsupported operation: " + operation
                );
            };

            return result.stripTrailingZeros().toPlainString();

        } catch (Exception e) {
            return "Calculation error: " + e.getMessage();
        }
    }
}
