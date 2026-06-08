package octo.cm.exception.business;

import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.WorkbenchBaseException;

public class UserException extends WorkbenchBaseException {
    // 前缀
    public static final String ERROR_CODE_PREFIX = "ERR_USER_";

    public UserException(ErrorDto errorDto) {
        super(errorDto);
    }

    public static class Builder {

        // 用户模型Id,用户名不得为空
        public static UserException userModelIdOrNameEmpty() {
            return new UserException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00001"),
                            "用户模型Id,用户名不得为空")
            );
        }

        // 组织模型Id,用户模型Id,用户编号不得为空
        public static UserException orgOrUserModelIdOrCodeEmpty() {
            return new UserException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00002"),
                            "组织模型Id,用户模型Id,用户编号不得为空")
            );
        }

        // 角色不得为空
        public static UserException roleEmpty() {
            return new UserException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00003"),
                            "角色不得为空")
            );
        }

        // 不存在用户
        public static UserException notFoundWithName(String userName) {
            return new UserException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00004"),
                            StrUtil.format("不存在用户:{}", userName))
            );
        }

    }

}
