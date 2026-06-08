package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;
import java.util.List;

public class CallExprDto implements Serializable {
    private String name;
    private List<String> args;

    public String getName() {
        return name;
    }

    public CallExprDto setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getArgs() {
        return args;
    }

    public CallExprDto setArgs(List<String> args) {
        this.args = args;
        return this;
    }
}
