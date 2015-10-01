package com.magnet.mmx.server.plugin.mmxmgmt.db.utils;

import com.magnet.mmx.server.plugin.mmxmgmt.db.MessageDAOImplTest;
import org.dbunit.database.DatabaseConfig;

import java.util.HashMap;
import java.util.Map;

public class TestDataSource {
  public static final TestDataSource APP_DATA_1 = new TestDataSource("/data/app-data-1.xml");
  public static final TestDataSource DEVICE_DATA_1 = new TestDataSource("/data/device-data-1.xml");
  public static final TestDataSource MESSAGE_DATA_1 = new TestDataSource.TestDataSourceBuilder().name("/data/message-data-1.xml").property(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MessageDAOImplTest.CustomDataTypeFactory()).build();
  public static final TestDataSource PUBSUB_NODE_DATA_1 = new TestDataSource("/data/pubsub-node-data-1.xml");
  public static final TestDataSource PUBSUB_TAG_DATA_1 = new TestDataSource("/data/pubsub-tag-data-1.xml");
  public static final TestDataSource PUSH_MESSAGE_DATA_1 = new TestDataSource("/data/pushmessage-data-1.xml");
  public static final TestDataSource TAG_DATA_1 = new TestDataSource("/data/tag-data-1.xml");
  public static final TestDataSource USER_DATA_1 = new TestDataSource("/data/user-data-1.xml");
  public static final TestDataSource USER_TAG_DATA_1 = new TestDataSource("/data/user-tags-1.xml");
  public static final TestDataSource WAKEUP_QUEUE_DATA_1 = new TestDataSource("/data/wakeup-queue-1.xml");

  private String name;
  private Map<String, Object> properties;

  protected TestDataSource() {
  }

  public TestDataSource(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public static class TestDataSourceBuilder {
    private TestDataSource toBuild = new TestDataSource();

    public TestDataSourceBuilder name(String value) {
      toBuild.name = value;
      return this;
    }

    public TestDataSourceBuilder property(String key, Object value) {
      if(null == toBuild.properties) {
        toBuild.properties = new HashMap<String, Object>();
      }
      toBuild.properties.put(key, value);
      return this;
    }

    public TestDataSource build() {
      return toBuild;
    }
  }
}
