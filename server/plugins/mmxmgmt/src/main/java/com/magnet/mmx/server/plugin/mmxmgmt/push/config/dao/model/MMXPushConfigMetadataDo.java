package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by mmicevic on 3/31/16.
 *
 */

@Entity
@Table(name = "mmxPushConfigMetadata")
public class MMXPushConfigMetadataDo implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer metadataId;
    private Integer configId;
    private String name;
    private String value;

    public Integer getMetadataId() {
        return metadataId;
    }
    public void setMetadataId(Integer metadataId) {
        this.metadataId = metadataId;
    }

    public Integer getConfigId() {
        return configId;
    }
    public void setConfigId(Integer configId) {
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
