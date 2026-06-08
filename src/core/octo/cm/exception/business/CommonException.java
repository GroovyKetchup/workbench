package octo.cm.exception.business;

import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.WorkbenchBaseException;

public class CommonException extends WorkbenchBaseException {
    // 前缀
    public static final String ERROR_CODE_PREFIX = "ERR_CMN_";

    public CommonException(ErrorDto errorDto) {
        super(errorDto);
    }

    public static class Builder {

        // ==================== 参数校验相关 ====================

        // 请传递JSON数据
        public static CommonException jsonDataEmpty() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00001"),
                            "请传递JSON数据")
            );
        }

        // 非法的JSON数据
        public static CommonException invalidJsonData() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00002"),
                            "非法的JSON数据")
            );
        }

        // 请传递节点类型
        public static CommonException nodeTypeEmpty() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00003"),
                            "请传递节点类型")
            );
        }

        // 不支持的节点类型
        public static CommonException nodeTypeNotSupported(String nodeType) {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00004"),
                            StrUtil.format("不支持的节点类型[{}]", nodeType))
            );
        }

        // 请传递节点的编号
        public static CommonException nodeCodeEmpty() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00005"),
                            "请传递节点的编号")
            );
        }

        // 根据节点编号无法找到对应的节点
        public static CommonException nodeNotFoundWithCode(String nodeCode) {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00006"),
                            StrUtil.format("根据节点编号[{}]，无法找到对应的节点", nodeCode))
            );
        }

        // ==================== 大模型/LLM相关 ====================

        // 汇总失败，前序任务节点未正常产出任何数据
        public static CommonException llmCollectFailed() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00007"),
                            "汇总失败，前序任务节点未正常产出任何数据")
            );
        }

        // 无法获取节点代码生成的提示词
        public static CommonException llmPromptNotFound() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00008"),
                            "无法获取节点代码生成的提示词")
            );
        }

        // ==================== 其他通用异常 ====================

        // 没有获得正确的Callback
        public static CommonException invalidCallback() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00009"),
                            "没有获得正确的Callback")
            );
        }

        // 无法识别这个节点类型
        public static CommonException unrecognizedNodeType() {
            return new CommonException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00010"),
                            "无法识别这个节点类型")
            );
        }

    }

}
