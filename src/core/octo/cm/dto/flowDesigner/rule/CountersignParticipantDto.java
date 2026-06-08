package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;

public class CountersignParticipantDto implements Serializable {
    private String userCode;
    private String userName;
    private String status;
    private String action;
    private String opinion;
    private String time;

    public String getUserCode() {
        return userCode;
    }

    public CountersignParticipantDto setUserCode(String userCode) {
        this.userCode = userCode;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public CountersignParticipantDto setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public CountersignParticipantDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getAction() {
        return action;
    }

    public CountersignParticipantDto setAction(String action) {
        this.action = action;
        return this;
    }

    public String getOpinion() {
        return opinion;
    }

    public CountersignParticipantDto setOpinion(String opinion) {
        this.opinion = opinion;
        return this;
    }

    public String getTime() {
        return time;
    }

    public CountersignParticipantDto setTime(String time) {
        this.time = time;
        return this;
    }
}
