package octo.cm.exception.business;

import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.WorkbenchBaseException;

public class ViewException extends WorkbenchBaseException {
    // 前缀
    public static final String ERROR_CODE_PREFIX = "ERR_VIEW_";

    public ViewException(ErrorDto errorDto) {
        super(errorDto);
    }

    public static class Builder {

        // 当前菜单绑定视图无法获取到模型ID
        public static ViewException cannotGetModelId(String viewActionModel) {
            return new ViewException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00001"),
                            StrUtil.format("当前菜单绑定视图[{}]无法获取到模型ID", viewActionModel))
            );
        }

        // 当前菜单绑定模型ID非CM体系构建
        public static ViewException notCMModel(String modelId) {
            return new ViewException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00002"),
                            StrUtil.format("当前菜单绑定模型ID[{}]非CM体系构建", modelId))
            );
        }

        // 无法找到CM
        public static ViewException cmNotFound(String modelId) {
            return new ViewException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00003"),
                            StrUtil.format("无法找到CM[{}]", modelId))
            );
        }

        // 视图不存在
        public static ViewException notExist(String viewModelId, String viewInstCode) {
            return new ViewException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00004"),
                            StrUtil.format("视图[{}({})]不存在", viewModelId, viewInstCode))
            );
        }

        // 找不到该模型对应的工作空间
        public static ViewException workspaceNotFound() {
            return new ViewException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00005"),
                            "找不到该模型对应的工作空间")
            );
        }

        // 工作空间编号及模型的英文名称不得为空
        public static ViewException workspaceCodeOrEnNameEmpty() {
            return new ViewException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00006"),
                            "工作空间编号及模型的英文名称不得为空")
            );
        }

    }

}
