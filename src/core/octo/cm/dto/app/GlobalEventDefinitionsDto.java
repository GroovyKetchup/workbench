package octo.cm.dto.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用级全局事件定义列表及乐观锁版本。
 */
public class GlobalEventDefinitionsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String revision;
    private List<GlobalEventDefinitionRecordDto> events = new ArrayList<>();

    public String getRevision() {
        return revision;
    }

    public GlobalEventDefinitionsDto setRevision(String revision) {
        this.revision = revision;
        return this;
    }

    public List<GlobalEventDefinitionRecordDto> getEvents() {
        return events;
    }

    public GlobalEventDefinitionsDto setEvents(List<GlobalEventDefinitionRecordDto> events) {
        this.events = events == null ? new ArrayList<>() : events;
        return this;
    }
}
