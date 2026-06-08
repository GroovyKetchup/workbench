package octo.cm.util;

import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextTemplateUtil {


    // 使用正则表达式匹配 {{...}} 格式的占位符
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^\\}]+)\\}\\}");

    /**
     * 替换模板中的占位符
     * 注意：paramMap的Key无需使用花括号包裹
     *
     * @param template 模板字符串
     * @param paramMap 替换数据，键为占位符名称，值为替换内容
     * @return 替换后的字符串
     */
    public static String fillInParams(String template, Map<String, String> paramMap) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1); // 获取占位符名称
            String replacement = paramMap.getOrDefault(placeholder, "{{" + placeholder + "}}"); // 获取替换值，如果没有则保留原占位符
            if (StrUtil.isBlank(replacement)) continue;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }


    private static String test = "你将扮演一个聊天群聊天，我会提供给你群聊的规则，以及你所扮演的角色，最终再提供给你当前聊天群的对话信息，最终我期望你会按照以上内容进行答复用户。\n" +
            "\n" +
            "本群的群组定义是：\n" +
            "{{群组定义}}\n" +
            "\n";

    public static void main(String[] args) {

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("群组定义", "22323群组定义12!#$%");
        System.out.println(fillInParams(test, dataMap));
    }


}
