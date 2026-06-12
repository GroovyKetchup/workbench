package octo.cm.dto.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用级IP白名单配置。
 * <p>
 * 该DTO用于工具API的输入输出转换；底层保存到应用扩展配置中的
 * {@code 启用IP白名单访问控制} 与 {@code IP访问白名单} 两个标准 AppViewSetting 项。
 */
public class IpWhitelistConfigDto implements Serializable {
    /**
     * 是否启用IP白名单校验。
     */
    private Boolean enabled = false;
    /**
     * 白名单规则列表，支持精确IP、CIDR网段和通配符。
     */
    private List<String> items = new ArrayList<>();

    /**
     * 获取是否启用IP白名单校验。
     *
     * @return 启用时返回 true
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用IP白名单校验。
     *
     * @param enabled 是否启用
     * @return 当前配置对象
     */
    public IpWhitelistConfigDto setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * 获取白名单规则列表。
     *
     * @return 白名单规则列表
     */
    public List<String> getItems() {
        return items;
    }

    /**
     * 设置白名单规则列表。
     *
     * @param items 白名单规则列表
     * @return 当前配置对象
     */
    public IpWhitelistConfigDto setItems(List<String> items) {
        this.items = items;
        return this;
    }
}
