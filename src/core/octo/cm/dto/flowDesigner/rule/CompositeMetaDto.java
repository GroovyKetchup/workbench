package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;

public class CompositeMetaDto implements Serializable {
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public CompositeMetaDto setVersion(Integer version) {
        this.version = version;
        return this;
    }
}
