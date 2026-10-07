package host.plas.linkparser;

import singularity.configs.ModularizedConfig;

/**
 * {@code config.yml}: who may post links, and how the replaced chat line looks.
 */
public class LinkParserConfig extends ModularizedConfig {
    public LinkParserConfig() {
        super(LinkParser.getInstance(), "config.yml", true);
        init();
    }

    @Override
    public void init() {
        getPermission();
        isBareDomains();
        getFormat();
        getLinkHover();
        getColorPermission();
        isProxyEnabled();
        isProxyNetworkScope();
    }

    public String getPermission() {
        reloadResource();
        return getOrSetDefault("permission", "streamline.linkparser.use");
    }

    public boolean isBareDomains() {
        reloadResource();
        return getOrSetDefault("bare-domains", true);
    }

    public String getFormat() {
        reloadResource();
        return getOrSetDefault("format", "%streamline_user_formatted%&8: &f%message%");
    }

    public String getLinkHover() {
        reloadResource();
        return getOrSetDefault("link-hover", "#AAAAAAClick to open #FFFFFF%url%");
    }

    public String getColorPermission() {
        reloadResource();
        return getOrSetDefault("color-permission", "streamline.linkparser.colors");
    }

    public boolean isProxyEnabled() {
        reloadResource();
        return getOrSetDefault("proxy.enabled", false);
    }

    /** Whether a proxy sends the line to the whole network rather than the sender's server. */
    public boolean isProxyNetworkScope() {
        reloadResource();
        return getOrSetDefault("proxy.scope", "server").trim().equalsIgnoreCase("network");
    }
}
