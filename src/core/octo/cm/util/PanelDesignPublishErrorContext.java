package octo.cm.util;

import cmn.util.TraceUtil;
import cn.hutool.core.collection.CollUtil;
import com.leavay.dfc.gui.LvUtil;
import octo.cm.dto.ErrorDto;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;

public class PanelDesignPublishErrorContext {

    // 使用 ThreadLocal 来存储每个线程独立的错误列表
    private static final ThreadLocal<List<ErrorDto>> errors = new ThreadLocal<>();

    /**
     * 向当前线程的错误列表中添加一个错误信息。
     * 如果列表不存在，则会创建一个新的。
     */
    public static void addError(ErrorDto error) {
        if (errors.get() == null) {
            errors.set(new ArrayList<>());
        }
        errors.get().add(error);

        LvUtil.trace(
                ExceptionUtils.getFullStackTrace(error.getError())
        );


    }

    /**
     * 获取当前线程的错误列表。
     *
     * @return 如果存在错误列表，则返回该列表；否则返回 null。
     */
    public static List<ErrorDto> getErrors() {
        return errors.get();
    }

    /**
     * 获取当前线程的错误列表并进行清除
     *
     * @return 如果存在错误列表，则返回该列表；否则返回 null。
     */
    public static List<ErrorDto> getErrorsAndClear() {
        List<ErrorDto> errorDtos = errors.get();
        clear();
        return errorDtos;
    }

    /**
     * 获取当前线程的错误列表。
     *
     * @return 如果存在错误列表，则返回该列表；否则返回 null。
     */
    public static boolean hasError() {
        return !CollUtil.isEmpty(errors.get());
    }

    /**
     * 关键步骤：清理当前线程的 ThreadLocal 变量。
     * 这必须在每个请求结束时调用，以防止数据泄露。
     */
    public static void clear() {
        errors.remove();
    }
}