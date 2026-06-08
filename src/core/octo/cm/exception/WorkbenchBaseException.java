package octo.cm.exception;

import cmn.enums.ErrorLevel;
import cmn.exception.BaseException;
import octo.cm.dto.ErrorDto;

public abstract class WorkbenchBaseException extends BaseException {


    public WorkbenchBaseException(ErrorDto errorDto) {
        super(ErrorLevel.ERROR, errorDto.getErrorKey(), errorDto.getErrorContent());
//        super(ErrorLevel.WARN, errorDto.getErrorKey(), errorDto.getErrorContent());
    }

    public WorkbenchBaseException(String errCode, String errMsg) {
        super(ErrorLevel.ERROR, errCode, errMsg);
    }

    protected WorkbenchBaseException() {
    }


    public static class Builder {

    }

    public static String formatErrorCode(String prefix, String code) {
        return prefix + code;
    }

}
