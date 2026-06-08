package octo.cm.dto.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IpWhitelistConfigDto implements Serializable {
    private Boolean enabled = false;
    private List<String> items = new ArrayList<>();

    public Boolean getEnabled() {
        return enabled;
    }

    public IpWhitelistConfigDto setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public List<String> getItems() {
        return items;
    }

    public IpWhitelistConfigDto setItems(List<String> items) {
        this.items = items;
        return this;
    }
}
