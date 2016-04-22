package com.magnet.mmx.server.plugin.mmxmgmt.push.config;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate.Hibernate;

/**
 * Created by mmicevic on 4/20/16.
 *
 */
public class HibernateTestInitalizer {

    private static HibernateTestInitalizer instance = new HibernateTestInitalizer();

    public static HibernateTestInitalizer getInstance() {
        return instance;
    }

    private HibernateTestInitalizer() {
        String driver = "org.mariadb.jdbc.Driver";
        String url = "jdbc:mysql://127.0.0.1:3306/maxdb";
        String username = "root";
        String password = "";
        Hibernate.initialize(driver, url, username, password);
    }
}
