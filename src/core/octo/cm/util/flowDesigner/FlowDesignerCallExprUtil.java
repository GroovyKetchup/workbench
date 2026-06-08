package octo.cm.util.flowDesigner;

import octo.cm.dto.flowDesigner.rule.CallExprDto;

import java.util.ArrayList;
import java.util.List;

public class FlowDesignerCallExprUtil {

    public static String buildCall(String name, List<String> args) {
        if (isBlank(name)) throw new IllegalArgumentException("函数名不能为空");
        StringBuilder sb = new StringBuilder();
        sb.append(name.trim()).append('(');
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('\'').append(escapeArg(args.get(i))).append('\'');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    public static String escapeArg(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static String unescapeArg(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                sb.append(ch);
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                sb.append(ch);
            }
        }
        if (escaping) sb.append('\\');
        return sb.toString();
    }

    public static CallExprDto parseCall(String expr) {
        if (isBlank(expr)) throw new IllegalArgumentException("调用表达式不能为空");
        String source = expr.trim();
        int openIndex = source.indexOf('(');
        int closeIndex = source.lastIndexOf(')');
        if (openIndex <= 0 || closeIndex != source.length() - 1 || closeIndex < openIndex) {
            throw new IllegalArgumentException("非法的调用表达式：" + expr);
        }
        String name = source.substring(0, openIndex).trim();
        if (isBlank(name)) throw new IllegalArgumentException("调用表达式缺少函数名：" + expr);
        String body = source.substring(openIndex + 1, closeIndex).trim();
        return new CallExprDto().setName(name).setArgs(parseArgs(body));
    }

    private static List<String> parseArgs(String body) {
        List<String> args = new ArrayList<>();
        if (isBlank(body)) return args;
        int index = 0;
        while (index < body.length()) {
            index = skipSpaces(body, index);
            if (index >= body.length()) break;
            ParseResult result;
            if (body.charAt(index) == '\'') {
                result = parseQuotedArg(body, index);
            } else {
                result = parseBareArg(body, index);
            }
            args.add(result.value);
            index = skipSpaces(body, result.nextIndex);
            if (index >= body.length()) break;
            if (body.charAt(index) != ',') {
                throw new IllegalArgumentException("调用表达式参数分隔符非法：" + body);
            }
            index++;
        }
        return args;
    }

    private static ParseResult parseQuotedArg(String body, int startIndex) {
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        int index = startIndex + 1;
        while (index < body.length()) {
            char ch = body.charAt(index);
            if (escaping) {
                sb.append(ch);
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else if (ch == '\'') {
                return new ParseResult(sb.toString(), index + 1);
            } else {
                sb.append(ch);
            }
            index++;
        }
        throw new IllegalArgumentException("调用表达式参数缺少结束引号：" + body);
    }

    private static ParseResult parseBareArg(String body, int startIndex) {
        int index = startIndex;
        while (index < body.length() && body.charAt(index) != ',') {
            index++;
        }
        return new ParseResult(unescapeArg(body.substring(startIndex, index).trim()), index);
    }

    private static int skipSpaces(String value, int startIndex) {
        int index = startIndex;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class ParseResult {
        private final String value;
        private final int nextIndex;

        private ParseResult(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}
