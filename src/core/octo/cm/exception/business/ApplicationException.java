package octo.cm.exception.business;

import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.WorkbenchBaseException;

public class ApplicationException extends WorkbenchBaseException {
    // 前缀
    public static final String ERROR_CODE_PREFIX = "ERR_APP_";

    public ApplicationException(ErrorDto errorDto) {
        super(errorDto);
    }

    public static class Builder {

        // 未设置默认的发布应用
        public static ApplicationException defaultAppNotSet() {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00001"),
                            "你还没有设置默认的发布应用")
            );
        }

        // 当前业务域还未设置默认的应用
        public static ApplicationException domainDefaultAppNotSet() {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00002"),
                            "当前业务域还未设置默认的应用")
            );
        }

        // 要添加的视图信息不完整
        public static ApplicationException viewInfoIncomplete() {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00003"),
                            "要添加的视图信息不完整,视图名称、视图编号以及视图实例编号不得为空")
            );
        }

        // 目标应用的编号为空
        public static ApplicationException appCodeEmpty() {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00004"),
                            "应用编号不得为空")
            );
        }

        // 视图的名称不得为空
        public static ApplicationException viewNameEmpty() {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00005"),
                            "视图的名称不得为空")
            );
        }

        // 未找到编号为[code]的应用
        public static ApplicationException notFoundWithCode(String appCode) {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00006"),
                            StrUtil.format("未找到编号为[{}]的应用", appCode))
            );
        }

        // 应用不存在
        public static ApplicationException notExist() {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00007"),
                            "应用不存在")
            );
        }

        // 请选择要清空的应用
        public static ApplicationException selectAppToClear() {
            return new ApplicationException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00008"),
                            "请选择要清空的应用")
            );
        }

    }

}
