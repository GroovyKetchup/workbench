package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;

public class CountersignApprovalDto implements Serializable {
    private String type;
    private Integer value;

    public String getType() {
        return type;
    }

    public CountersignApprovalDto setType(String type) {
        this.type = type;
        return this;
    }

    public Integer getValue() {
        return value;
    }

    public CountersignApprovalDto setValue(Integer value) {
        this.value = value;
        return this;
    }
}
