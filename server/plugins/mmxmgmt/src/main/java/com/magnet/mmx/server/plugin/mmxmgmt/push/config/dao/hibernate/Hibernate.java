package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

/**
 * Created by mmicevic on 4/20/16.
 *
 */
public class Hibernate {

    private static SessionFactory sessionFactory;

    //        String driver = JiveGlobals.getProperty("database.defaultProvider.driver");
    //        String serverURL = JiveGlobals.getProperty("database.defaultProvider.serverURL");
    //        String username = JiveGlobals.getProperty("database.defaultProvider.username");
    //        String password = JiveGlobals.getProperty("database.defaultProvider.password");


//    public static void initialize() {
//
//        if (sessionFactory != null) {
//            throw new RuntimeException("HIBERNATE ALREADY INITIALIZED");
//        }
//
//        Configuration configuration = new Configuration().configure();
//        System.out.println("CONFIGURED");
//    }
    public static void initialize(String driver, String url, String username, String password) {

        if (sessionFactory != null) {
            throw new RuntimeException("HIBERNATE ALREADY INITIALIZED");
        }

        Configuration configuration = new Configuration().configure();
        System.out.println("CONFIGURED");
        configuration.setProperty("connection.driver_class", driver);
        System.out.println("SET PROPERTY DRIVER = " + driver);
        configuration.setProperty("hibernate.connection.url", url);
        System.out.println("SET PROPERTY URL = " + url);
        configuration.setProperty("hibernate.connection.username", username);
        System.out.println("SET PROPERTY USERNAME = " + username);
        configuration.setProperty("hibernate.connection.password", password);
        System.out.println("SET PROPERTY PASSWORD = " + password);

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties());
        sessionFactory = configuration.buildSessionFactory(builder.build());
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new RuntimeException("HIBERNATE NOT INITIALIZED");
        }
        return sessionFactory;
    }
}
