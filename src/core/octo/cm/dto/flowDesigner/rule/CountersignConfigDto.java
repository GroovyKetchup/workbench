package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CountersignConfigDto implements Serializable {
    private String field;
    private List<String> roleCodes;
    private CountersignApprovalDto approval;
    private Map<String, String> userActions;
    private Map<String, String> routes;

    public String getField() {
        return field;
    }

    public CountersignConfigDto setField(String field) {
        this.field = field;
        return this;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public CountersignConfigDto setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
        return this;
    }

    public CountersignApprovalDto getApproval() {
        return approval;
    }

    public CountersignConfigDto setApproval(CountersignApprovalDto approval) {
        this.approval = approval;
        return this;
    }

    public Map<String, String> getUserActions() {
        return userActions;
    }

    public CountersignConfigDto setUserActions(Map<String, String> userActions) {
        this.userActions = userActions;
        return this;
    }

    public Map<String, String> getRoutes() {
        return routes;
    }

    public CountersignConfigDto setRoutes(Map<String, String> routes) {
        this.routes = routes;
        return this;
    }
}
