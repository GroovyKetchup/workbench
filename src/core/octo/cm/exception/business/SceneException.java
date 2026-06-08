package octo.cm.exception.business;

import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.WorkbenchBaseException;

public class SceneException extends WorkbenchBaseException {
    // 前缀
    public static final String ERROR_CODE_PREFIX = "ERR_SCN_";

    public SceneException(ErrorDto errorDto) {
        super(errorDto);
    }

    public static class Builder {

        // 场景编号为空
        public static SceneException sceneCodeEmpty() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00001"),
                            "场景编号不得为空")
            );
        }

        // 无法找到编号为[code]的场景
        public static SceneException notFoundWithCode(String sceneCode) {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00002"),
                            StrUtil.format("无法找到编号为[{}]的场景", sceneCode))
            );
        }

        // 当前场景没有设置场景类型
        public static SceneException categoryNotSet(String sceneCode) {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00003"),
                            StrUtil.format("当前场景[{}]没有设置场景类型", sceneCode))
            );
        }

        // 场景编号以及数据编号不得同时为空
        public static SceneException sceneAndDataCodeBothEmpty() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00004"),
                            "场景编号以及数据编号不得同时为空")
            );
        }

        // 场景编号以及行为编号不得同时为空
        public static SceneException sceneAndBehaviorCodeBothEmpty() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00005"),
                            "场景编号以及行为编号不得同时为空")
            );
        }

        // 场景编号以及约束编号不得同时为空
        public static SceneException sceneAndConstraintCodeBothEmpty() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00006"),
                            "场景编号以及约束编号不得同时为空")
            );
        }

        // 场景编号以及编排编号不得同时为空
        public static SceneException sceneAndOrchestrationCodeBothEmpty() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00007"),
                            "场景编号以及编排编号不得同时为空")
            );
        }

        // 场景编号以及展示编号不得同时为空
        public static SceneException sceneAndDisplayCodeBothEmpty() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00008"),
                            "场景编号以及展示编号不得同时为空")
            );
        }

        // 无法确定要发布的目标场景
        public static SceneException cannotDetermineTarget() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00009"),
                            "无法确定要发布的目标场景")
            );
        }

        // 无法确定要发布哪些场景
        public static SceneException cannotDetermineWhichToPublish() {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00010"),
                            "无法确定要发布哪些场景")
            );
        }

        // 模块未设定上层模块或上层场景
        public static SceneException upperModuleOrSceneNotSet(String label) {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00011"),
                            StrUtil.format("模块[{}]未设定上层模块或上层场景", label))
            );
        }

        // 模块未设定上层系统或上层模块
        public static SceneException upperSystemOrModuleNotSet(String label) {
            return new SceneException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00012"),
                            StrUtil.format("模块[{}]未设定上层系统或上层模块", label))
            );
        }

    }


}
