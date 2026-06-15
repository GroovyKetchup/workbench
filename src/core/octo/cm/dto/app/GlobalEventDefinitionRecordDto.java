package octo.cm.dto.app;

import java.io.Serializable;

/**
 * 应用级全局事件定义落库记录。
 */
public class GlobalEventDefinitionRecordDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventCode;
    private String eventName;
    private String definitionJson;

    public String getEventCode() {
        return eventCode;
    }

    public GlobalEventDefinitionRecordDto setEventCode(String eventCode) {
        this.eventCode = eventCode;
        return this;
    }

    public String getEventName() {
        return eventName;
    }

    public GlobalEventDefinitionRecordDto setEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public String getDefinitionJson() {
        return definitionJson;
    }

    public GlobalEventDefinitionRecordDto setDefinitionJson(String definitionJson) {
        this.definitionJson = definitionJson;
        return this;
    }
}
