package octo.cm.exception.business;

import cmn.enums.ErrorLevel;
import cmn.exception.MultiException;
import cn.hutool.core.util.StrUtil;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.WorkbenchBaseException;

import java.util.List;

public class PanelDesignException extends WorkbenchBaseException {
    // 前缀
    public static final String ERROR_CODE_PREFIX = "ERR_PANEL_";


    public PanelDesignException(ErrorDto errorDto) {
        super(errorDto);
    }

    public static class Builder {

        // 面板编号不得为空
        public static PanelDesignException panelCodeEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00001"),
                            "面板编号不得为空")
            );
        }

        // 面板设计表单不得为空
        public static PanelDesignException formEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00002"),
                            "面板设计表单不得为空")
            );
        }

        // 无法找到面板编号为[code]的面板设计
        public static PanelDesignException notFoundWithCode(String panelCode) {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00003"),
                            StrUtil.format("无法找到面板编号为[{}]的面板设计", panelCode))
            );
        }

        // 面板分类不得为空
        public static PanelDesignException categoryEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00004"),
                            "面板分类不得为空")
            );
        }

        // 无法找到面板分类
        public static PanelDesignException categoryNotFoundWithCode(String code) {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00005"),
                            StrUtil.format("无法找到面板分类，编号[{}]", code))
            );
        }

        // 你至少需要选择一个面板
        public static PanelDesignException atLeastSelectOne() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00006"),
                            "你至少需要选择一个面板")
            );
        }

        // 没有任何面板设计需要被发布
        public static PanelDesignException noPanelToPublish() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00007"),
                            "没有任何面板设计需要被发布")
            );
        }

        // 没有任何面板设计需要被初始化
        public static PanelDesignException noPanelToInit() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00008"),
                            "没有任何面板设计需要被初始化")
            );
        }

        // 请先保存当前面板
        public static PanelDesignException saveFirst() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00009"),
                            "请先保存当前面板")
            );
        }

        // 未知的视图类型
        public static PanelDesignException unknownViewType(String action) {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00010"),
                            StrUtil.format("未知的视图类型，无法{}", action))
            );
        }

        // 要发布面板的场景编号和面板编号必须提供一种
        public static PanelDesignException sceneOrPanelCodeRequired() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00011"),
                            "要发布面板的场景编号和面板编号必须提供一种")
            );
        }

        // 无法确定要发布的目标面板
        public static PanelDesignException cannotDetermineTarget() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00012"),
                            "无法确定要发布的目标面板")
            );
        }

        // 暂不支持面板分类进行初始化
        public static PanelDesignException categoryNotSupported(String categoryName) {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00013"),
                            StrUtil.format("暂不支持面板分类[{}]进行初始化", categoryName))
            );
        }

        // 面板数据不得为空
        public static PanelDesignException panelDataEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00014"),
                            "面板数据不得为空")
            );
        }

        // 大模型未返回面板状态
        public static PanelDesignException panelStatusEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00015"),
                            "大模型未返回面板状态")
            );
        }

        // 无法找到之前初始化的面板
        public static PanelDesignException notFoundByScene(String sceneCode) {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00016"),
                            StrUtil.format("在进行大模型初始化面板设计时，无法找到之前初始化的面板，所属场景为:{}", sceneCode))
            );
        }

        // 面板编号或面板名称不得为空
        public static PanelDesignException codeOrNameEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00017"),
                            "面板编号或面板名称不得为空")
            );
        }

        // 无法找到面板设计数据
        public static PanelDesignException notFoundWithName(String name) {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00018"),
                            StrUtil.format("无法找到面板设计数据[{}]", name))
            );
        }

        // 没找到任何要发布的面板
        public static PanelDesignException notFoundAnyToPublish() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00019"),
                            "没找到任何要发布的面板")
            );
        }

        // 生效面板仅运行在表单上
        public static PanelDesignException onlyRunOnFormView() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00020"),
                            "生效面板仅运行在表单上")
            );
        }

        // 没有异常，但面板编号为空
        public static PanelDesignException noExceptionButCodeEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00021"),
                            "没有异常，但面板编号为空")
            );
        }

        // 无法得知生成哪一种视图的实例编号
        public static PanelDesignException cannotDetermineViewType() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00022"),
                            "无法得知生成哪一种视图的实例编号")
            );
        }

        // 页面入口为空
        public static PanelDesignException pageEntryEmpty() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00023"),
                            "页面入口为空")
            );
        }

        // 没有要发布的页面
        public static PanelDesignException noPageToPublish() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00024"),
                            "没有要发布的页面，请先进行初始化")
            );
        }

        // 暂不支持发布表单
        public static PanelDesignException formPublishNotSupported() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00025"),
                            "暂不支持发布表单")
            );
        }


        // 批量发布错误
        public static MultiException batchPublishError(List<ErrorDto> errors) {
            MultiException multiException = new MultiException(ErrorLevel.ERROR, "00026", "发布面板设计错误");
            for (ErrorDto error : errors) {
                multiException.add(error.getError());
            }

            return multiException;

        }

        // 系统模块不支持发布场景
        public static PanelDesignException systemModulePublishNotSupported() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00027"),
                            "系统模块不支持发布场景")
            );
        }

        // 没有拿到发布面板设计的锁
        public static PanelDesignException failedToAcquirePublishLock() {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00028"),
                            "没有拿到发布面板设计的锁")
            );
        }

        // 面板编号已存在
        public static PanelDesignException panelCodeAlreadyExisted(String panelCode) {
            return new PanelDesignException(
                    ErrorDto.newWarn(formatErrorCode(ERROR_CODE_PREFIX, "00029"),
                            StrUtil.format("面板编号[{}]已存在，请勿重复创建", panelCode))
            );
        }


    }


}
