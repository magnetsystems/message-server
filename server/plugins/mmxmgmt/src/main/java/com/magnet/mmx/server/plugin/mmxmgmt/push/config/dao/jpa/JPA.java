package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Properties;

/**
 * Created by mmicevic on 4/20/16.
 *
 */
public class JPA {

    private static EntityManagerFactory entityManagerFactory;

    public static void initialize(String driver, String url, String username, String password) {

        if (entityManagerFactory != null) {
            throw new RuntimeException("JPA ALREADY INITIALIZED");
        }

        Properties properties = new Properties();
        properties.setProperty("javax.persistence.jdbc.driver", driver);
        properties.setProperty("javax.persistence.jdbc.url", url);
        properties.setProperty("javax.persistence.jdbc.user", username);
        properties.setProperty("javax.persistence.jdbc.password", password);

//        properties.setProperty("hibernate.connection.driver_class", driver);
//        properties.setProperty("hibernate.connection.url", url);
//        properties.setProperty("hibernate.connection.username", username);
//        properties.setProperty("hibernate.connection.password", password);

        entityManagerFactory = Persistence.createEntityManagerFactory("mmx_persistence", properties);
        System.out.println("JPA INITIALIZED SUCCESSFULLY");
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null) {
            throw new RuntimeException("JPA NOT INITIALIZED");
        }
        return entityManagerFactory;
    }
}
