package octo.cm.dto;

import cmn.anotation.ClassDeclare;
import cmn.enums.ErrorLevel;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("错误Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-01", updateTime = "2025-09-01"
)
public class ErrorDto implements Serializable {

    // 错误级别
    private ErrorLevel errorLevel;
    // 错误Key
    private String errorKey;
    // 错误堆栈
    private String errorContent;
    // 原始错误
    private Throwable error;

    public ErrorDto() {
    }

    public ErrorDto(String errorKey, String errorContent) {
        this.errorLevel = ErrorLevel.ERROR;
        this.errorKey = errorKey;
        this.errorContent = errorContent;
    }

    public ErrorDto(String errorKey, String errorContent, Throwable error) {
        this.errorLevel = ErrorLevel.ERROR;
        this.errorKey = errorKey;
        this.errorContent = errorContent;
        this.error = error;
    }


    // 构建警告级别的错误
    public static ErrorDto newWarn(String errorKey, String errorContent) {
        return new ErrorDto()
                .setErrorLevel(ErrorLevel.WARN)
                .setErrorKey(errorKey)
                .setErrorContent(errorContent)
                ;
    }

    // 构建错误级别的错误
    public static ErrorDto newError(String errorKey, String errorContent) {
        return new ErrorDto()
                .setErrorLevel(ErrorLevel.ERROR)
                .setErrorKey(errorKey)
                .setErrorContent(errorContent)
                ;
    }


    // ========================= getter/setter =========================


    public ErrorLevel getErrorLevel() {
        return errorLevel;
    }

    public ErrorDto setErrorLevel(ErrorLevel errorLevel) {
        this.errorLevel = errorLevel;
        return this;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public ErrorDto setErrorKey(String errorKey) {
        this.errorKey = errorKey;
        return this;
    }

    public String getErrorContent() {
        return errorContent;
    }

    public ErrorDto setErrorContent(String errorContent) {
        this.errorContent = errorContent;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    public ErrorDto setError(Throwable error) {
        this.error = error;
        return this;
    }
}
