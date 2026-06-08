package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CountersignInstanceDto implements Serializable {
    private String nodeId;
    private String field;
    private CountersignApprovalDto approval;
    private String status;
    private String result;
    private List<CountersignParticipantDto> participants;
    private Map<String, String> userActions;
    private Map<String, String> routes;
    private String createdAt;
    private String updatedAt;
    private Integer version;

    public String getNodeId() {
        return nodeId;
    }

    public CountersignInstanceDto setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public String getField() {
        return field;
    }

    public CountersignInstanceDto setField(String field) {
        this.field = field;
        return this;
    }

    public CountersignApprovalDto getApproval() {
        return approval;
    }

    public CountersignInstanceDto setApproval(CountersignApprovalDto approval) {
        this.approval = approval;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public CountersignInstanceDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getResult() {
        return result;
    }

    public CountersignInstanceDto setResult(String result) {
        this.result = result;
        return this;
    }

    public List<CountersignParticipantDto> getParticipants() {
        return participants;
    }

    public CountersignInstanceDto setParticipants(List<CountersignParticipantDto> participants) {
        this.participants = participants;
        return this;
    }

    public Map<String, String> getUserActions() {
        return userActions;
    }

    public CountersignInstanceDto setUserActions(Map<String, String> userActions) {
        this.userActions = userActions;
        return this;
    }

    public Map<String, String> getRoutes() {
        return routes;
    }

    public CountersignInstanceDto setRoutes(Map<String, String> routes) {
        this.routes = routes;
        return this;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public CountersignInstanceDto setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public CountersignInstanceDto setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public CountersignInstanceDto setVersion(Integer version) {
        this.version = version;
        return this;
    }
}
