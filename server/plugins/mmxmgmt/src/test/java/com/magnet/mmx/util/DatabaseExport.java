/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.magnet.mmx.util;

import com.magnet.mmx.server.plugin.mmxmgmt.db.DeviceDAOImplTest;
import org.apache.commons.dbcp2.BasicDataSource;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Helper class for dumping information from a database to an XML that DBUnit can process.
 */
public class DatabaseExport {




  public static void main(String[] args) throws Exception {
    InputStream inputStream =  DeviceDAOImplTest.class.getResourceAsStream("/test.properties");

    Properties testProperties = new Properties();
    testProperties.load(inputStream);

    String host = testProperties.getProperty("db.host");
    String port = testProperties.getProperty("db.port");
    String user = testProperties.getProperty("db.user");
    String password = testProperties.getProperty("db.password");
    String driver = testProperties.getProperty("db.driver");
    String schema = testProperties.getProperty("db.schema");

    String url = "jdbc:mysql://" + host + ":" + port + "/" + schema;

    BasicDataSource ds = new BasicDataSource();
    ds.setDriverClassName(driver);
    ds.setUsername(user);
    ds.setPassword(password);
    ds.setUrl(url);
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("mmxTag", "SELECT * FROM mmxTag ");
    doWork(ds.getConnection(), map);
  }

  public static void doWork(Connection conn, TreeMap<String, String> tableQueryMap) throws Exception
  {
    IDatabaseConnection connection = new DatabaseConnection(conn);

    // partial database export
    QueryDataSet partialDataSet = new QueryDataSet(connection);
    for (String table : tableQueryMap.keySet()) {
      partialDataSet.addTable(table, tableQueryMap.get(table));
    }

    FlatXmlDataSet.write(partialDataSet, System.out);
  }


}
