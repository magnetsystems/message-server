package com.magnet.mmx.server.plugin.mmxmgmt.db.utils;

import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplTest;
import com.magnet.mmx.server.plugin.mmxmgmt.db.UnitTestDSProvider;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.rules.ExternalResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class BaseDbTest {


  /**
   * clean any existing records and load some records into the database
   * @param dataResource
   * @throws Exception
   */
  protected static void loadData(DataSource ds, TestDataSource... dataResource) throws Exception {
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    for(TestDataSource ts : dataResource){
      InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream(ts.getName());
      if(null != ts.getProperties() && !ts.getProperties().isEmpty()) {
        DatabaseConfig dbConfig = con.getConfig();
        for(Map.Entry<String, Object> e : ts.getProperties().entrySet()) {
          dbConfig.setProperty(e.getKey(), e.getValue());
        }
      }
      IDataSet dataSet = builder.build(xmlInput);
      DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
    }
  }

  protected static void loadData(DataSource ds, String dataResource, Map<String, Object> properties) throws Exception {
    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    Connection setup = ds.getConnection();
    IDatabaseConnection con = new DatabaseConnection(setup);
    if(null != properties && !properties.isEmpty()) {
      DatabaseConfig dbConfig = con.getConfig();
      for(Map.Entry<String, Object> e : properties.entrySet()) {
        dbConfig.setProperty(e.getKey(), e.getValue());
      }
    }

    InputStream xmlInput = DeviceDAOImplTest.class.getResourceAsStream(dataResource);
    IDataSet dataSet = builder.build(xmlInput);
    DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
  }

  protected static void closeDataSource(DataSource ds) {
    try {
      System.out.println("---------------closeDataSource : " + ds);
      if(ds instanceof BasicDataSource) {
        ((BasicDataSource) ds).close();
      } else if(ds instanceof EmbeddedDatabase) {
        //((EmbeddedDatabase) ds).shutdown();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static class DataSourceResource extends ExternalResource {
    private final DataSource ds;
    private final TestDataSource[] dataResources;
    private Runnable beforeDataInit;
    private Runnable beforeDataSourceClosed;

    public DataSourceResource() {
      this(null);
    }

    public DataSourceResource(TestDataSource... dataResources) {
      this(null, null, dataResources);
    }

    public DataSourceResource(Runnable beforeDataInit, Runnable beforeDataSourceClosed, TestDataSource... dataResources) {
      this.beforeDataInit = beforeDataInit;
      this.beforeDataSourceClosed = beforeDataSourceClosed;
      ds = UnitTestDSProvider.getDataSource();
      this.dataResources = dataResources;
    }

    @Override
    protected void before() throws Throwable {
      if(null != beforeDataInit) {
        beforeDataInit.run();
      }

      if(null != dataResources) {
        loadData(ds, dataResources);
      }
      System.out.println("----------before()");
    };

    @Override
    protected void after() {
      if(null != beforeDataSourceClosed) {
        beforeDataSourceClosed.run();
      }

      closeDataSource(ds);
      System.out.println("----------after()");
    };

    public DataSource getDataSource() {
      return ds;
    }
  };
}
