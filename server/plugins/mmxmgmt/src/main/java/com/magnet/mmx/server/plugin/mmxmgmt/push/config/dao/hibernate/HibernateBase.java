package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.hibernate;

import org.hibernate.*;
import org.hibernate.criterion.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class HibernateBase<D> {

    private StatelessSession currentSession;
    private Transaction currentTransaction;
    private Class<D> clazz;
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateBase.class);

    public HibernateBase(Class<D> clazz) {
        this.clazz = clazz;
    }

//    private static SessionFactory getSessionFactory() {
//        Configuration configuration = new Configuration().configure();
//        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
//                .applySettings(configuration.getProperties());
//        SessionFactory sessionFactory = configuration.buildSessionFactory(builder.build());
//        return sessionFactory;
//    }


    private StatelessSession openCurrentSession() {
        currentSession = Hibernate.getSessionFactory().openStatelessSession();
//        currentSession.setFlushMode(FlushMode.AUTO);
        return currentSession;
    }
//    private Session openCurrentSession() {
//        try {
//            currentSession = Hibernate.getSessionFactory().withOptions().connection(DbConnectionManager.getConnection()).openSession();
//        }
//        catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return currentSession;
//    }

    private StatelessSession openCurrentSessionWithTransaction() {
        openCurrentSession();
        currentTransaction = currentSession.beginTransaction();
        return currentSession;
    }

    private void closeCurrentSession() {
        currentSession.close();
    }

    private StatelessSession getCurrentSession() {
        return currentSession;
    }

    private Transaction getCurrentTransaction() {
        return currentTransaction;
    }



    //CRUD
    public Serializable save(D doObject) {
        try {
            openCurrentSessionWithTransaction();
            Serializable id = getCurrentSession().insert(doObject);
            getCurrentTransaction().commit();
            return id;
        } catch (Throwable t){
            getCurrentTransaction().rollback();
            LOGGER.error("Hibernate error during save ", t);
            throw t;
        } finally {
            closeCurrentSession();
        }
    }
    public D findById(Serializable id) {
        try {
            openCurrentSession();
            return (D) getCurrentSession().get(clazz, id);
        } finally {
            closeCurrentSession();
        }
    }
    public D findSingleByCriteria(Criterion... restrictions) {
        try {
            openCurrentSession();
            Criteria criteria = getCurrentSession().createCriteria(clazz);
            if (restrictions != null) {
                for(Criterion restriction: restrictions) {
                    criteria.add(restriction);
                }
            }
            return (D) criteria.uniqueResult();
        } finally {
            closeCurrentSession();
        }
    }
    public List<D> findManyByCriteria(Criterion... restrictions) {
        try {
            openCurrentSession();
            Criteria criteria = getCurrentSession().createCriteria(clazz);
            if (restrictions != null) {
                for(Criterion restriction: restrictions) {
                    criteria.add(restriction);
                }
            }
            return (List<D>) criteria.list();
        } finally {
            closeCurrentSession();
        }
    }

    public void update(D doObject) {
        try {
            openCurrentSessionWithTransaction();
            getCurrentSession().update(doObject);
            getCurrentTransaction().commit();
        } catch (Throwable t){
            getCurrentTransaction().rollback();
            LOGGER.error("Hibernate error during update ", t);
            throw t;
        } finally {
            closeCurrentSession();
        }
    }
    public void delete(D doObject) {
        try {
            openCurrentSessionWithTransaction();
            getCurrentSession().delete(doObject);
            getCurrentTransaction().commit();
        } catch (Throwable t){
            getCurrentTransaction().rollback();
            LOGGER.error("Hibernate error during delete ", t);
            throw t;
        } finally {
            closeCurrentSession();
        }
    }
}
