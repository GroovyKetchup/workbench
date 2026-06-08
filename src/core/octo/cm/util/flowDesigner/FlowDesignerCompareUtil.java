package octo.cm.util.flowDesigner;

import java.math.BigDecimal;

public class FlowDesignerCompareUtil {
    public static final String OP_EQ = "=";
    public static final String OP_NE = "!=";
    public static final String OP_CONTAINS = "contains";
    public static final String OP_NOT_CONTAINS = "not_contains";
    public static final String OP_GT = ">";
    public static final String OP_GTE = ">=";
    public static final String OP_LT = "<";
    public static final String OP_LTE = "<=";

    public static final String MODE_VALUE = "value";
    public static final String MODE_FIELD = "field";

    public static Boolean compare(Object leftValue, String operator, Object rightValue) {
        String normalizedOperator = normalizeOperator(operator);
        if (isBlankValue(leftValue)) return compareBlankLeft(normalizedOperator, rightValue);
        if (OP_EQ.equals(normalizedOperator)) return equalsValue(leftValue, rightValue);
        if (OP_NE.equals(normalizedOperator)) return !equalsValue(leftValue, rightValue);
        if (OP_CONTAINS.equals(normalizedOperator)) return asString(leftValue).contains(asString(rightValue));
        if (OP_NOT_CONTAINS.equals(normalizedOperator)) return !asString(leftValue).contains(asString(rightValue));
        int compareResult = compareOrder(leftValue, rightValue);
        if (OP_GT.equals(normalizedOperator)) return compareResult > 0;
        if (OP_GTE.equals(normalizedOperator)) return compareResult >= 0;
        if (OP_LT.equals(normalizedOperator)) return compareResult < 0;
        if (OP_LTE.equals(normalizedOperator)) return compareResult <= 0;
        throw new IllegalArgumentException("不支持的比较运算符：" + operator);
    }

    public static void validateMode(String mode) {
        normalizeMode(mode);
    }

    public static String normalizeMode(String mode) {
        if (mode == null || mode.trim().isEmpty()) return MODE_VALUE;
        if (MODE_VALUE.equals(mode) || MODE_FIELD.equals(mode)) return mode;
        throw new IllegalArgumentException("不支持的比较模式：" + mode);
    }

    public static void validateOperator(String operator) {
        normalizeOperator(operator);
    }

    public static String normalizeOperator(String operator) {
        if (operator == null || operator.trim().isEmpty()) return OP_EQ;
        if (OP_EQ.equals(operator)
                || OP_NE.equals(operator)
                || OP_CONTAINS.equals(operator)
                || OP_NOT_CONTAINS.equals(operator)
                || OP_GT.equals(operator)
                || OP_GTE.equals(operator)
                || OP_LT.equals(operator)
                || OP_LTE.equals(operator)) return operator;
        throw new IllegalArgumentException("不支持的比较运算符：" + operator);
    }

    private static Boolean compareBlankLeft(String operator, Object rightValue) {
        if (OP_EQ.equals(operator)) return isBlankValue(rightValue);
        if (OP_NE.equals(operator)) return !isBlankValue(rightValue);
        return false;
    }

    private static boolean equalsValue(Object leftValue, Object rightValue) {
        if (isBlankValue(leftValue) || isBlankValue(rightValue)) return isBlankValue(leftValue) && isBlankValue(rightValue);
        BigDecimal leftNumber = toNumber(leftValue);
        BigDecimal rightNumber = toNumber(rightValue);
        if (leftNumber != null && rightNumber != null) return leftNumber.compareTo(rightNumber) == 0;
        return asString(leftValue).equals(asString(rightValue));
    }

    private static int compareOrder(Object leftValue, Object rightValue) {
        if (isBlankValue(rightValue)) return asString(leftValue).compareTo(asString(rightValue));
        BigDecimal leftNumber = toNumber(leftValue);
        BigDecimal rightNumber = toNumber(rightValue);
        if (leftNumber != null && rightNumber != null) return leftNumber.compareTo(rightNumber);
        return asString(leftValue).compareTo(asString(rightValue));
    }

    private static BigDecimal toNumber(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return new BigDecimal(String.valueOf(value));
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean isBlankValue(Object value) {
        return value == null || String.valueOf(value).trim().isEmpty();
    }
}
