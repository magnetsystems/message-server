package com.magnet.mmx.server.plugin.mmxmgmt.push.template.dao.model;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public class MMXPushConfigMetadataDo {

    private int metadataId;
    private int configId;
    private String name;
    private String value;

    public int getMetadataId() {
        return metadataId;
    }
    public void setMetadataId(int metadataId) {
        this.metadataId = metadataId;
    }

    public int getConfigId() {
        return configId;
    }
    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
