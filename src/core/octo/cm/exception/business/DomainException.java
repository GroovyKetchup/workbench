package octo.cm.exception.business;

import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.WorkbenchBaseException;

public class DomainException extends WorkbenchBaseException {

    // 前缀
    public static final String ERROR_CODE_PREFIX = "ERR_DOMAIN_";

    public DomainException(ErrorDto errorDto) {
        super(errorDto);
    }

    public static class Builder {

        // 业务域编号不得为空
        public static DomainException busDomainCodeEmpty() {
            return new DomainException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00001"),
                            "业务域编号不得为空")
            );
        }

        // 未找到对应的业务域
        public static DomainException notFound() {
            return new DomainException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00002"),
                            "未找到对应的业务域"));
        }

        // 未找到对应的业务域（带编号）
        public static DomainException notFoundWithCode(String busDomainCode) {
            return new DomainException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00003"),
                            StrUtil.format("无法找到对应的业务域[{}]", busDomainCode)));
        }

        // 未找到业务域Observer
        public static DomainException observerNotFound() {
            return new DomainException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00004"),
                            "无法获取当前业务域信息"));
        }

        // 无法确定目标业务域
        public static DomainException cannotDetermine() {
            return new DomainException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00005"),
                            "无法确定目标业务域"));
        }

        // 业务域编号和面板编号不得为空
        public static DomainException codeAndPanelCodeEmpty() {
            return new DomainException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00006"),
                            "业务域编号和面板编号不得为空"));
        }

        // 业务域或模型Id为空
        public static DomainException formModelIdEmpty() {
            return new DomainException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00007"),
                            "业务域或模型Id为空"));
        }

    }


}
