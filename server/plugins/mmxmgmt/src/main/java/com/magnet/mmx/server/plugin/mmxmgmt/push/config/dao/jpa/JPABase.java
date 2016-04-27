package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.jpa;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.*;

/**
 * Created by mmicevic on 4/15/16.
 *
 */
public class JPABase<D> {

    private Class<D> clazz;
    private static final Logger LOGGER = LoggerFactory.getLogger(JPABase.class);

    public JPABase(Class<D> clazz) {
        this.clazz = clazz;
    }


    //
//    EntityManager em = factory.createEntityManager();
//    // Read the existing entries and write to console
//    Query q = em.createQuery("SELECT u FROM User u");
//    List<User> userList = q.getResultList();
//    for (User user : userList) {
//        System.out.println(user.Name);
//    }
//    System.out.println("Size: " + userList.size());
//
//    // Create new user
//    em.getTransaction().begin();
//    User user = new User();
//    user.setName("Tom Johnson");
//    user.setLogin("tomj");
//    user.setPassword("pass");
//    em.persist(user);
//    em.getTransaction().commit();
//
//    em.close();
//


    //CRUD
    public void save(D doObject) {
        EntityManager em = JPA.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(doObject);
            em.getTransaction().commit();
        } catch (Throwable t){
            em.getTransaction().rollback();
            LOGGER.error("JPA error during save ", t);
            throw t;
        } finally {
            em.close();
        }
    }
    public D findById(Serializable id) {
        EntityManager em = JPA.getEntityManagerFactory().createEntityManager();
        try {
            return em.find(clazz, id);
        } finally {
            em.close();
        }
    }
    public D findSingleByCriteria(Map<String, Object> params) {
        List<D> list = findManyByCriteria(params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new RuntimeException("not single result");
        }
        return list.get(0);
//        EntityManager em = JPA.getEntityManagerFactory().createEntityManager();
//        try {
//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<D> q = cb.createQuery(clazz);
//            Root<D> from = q.from(clazz);
//            List<Predicate> predicates = new ArrayList<>();
//            for(String paramName : params.keySet()) {
//                Predicate p = cb.equal(from.get(paramName), params.get(paramName));
//                predicates.add(p);
//            }
//            Predicate[] arr = new Predicate[predicates.size()];
//            arr = predicates.toArray(arr);
//            q.select(from).where(cb.and(arr));
//            return em.createQuery(q).getSingleResult();
//        } finally {
//            em.close();
//        }
    }
    public List<D> findManyByCriteria(Map<String, Object> params) {
        EntityManager em = JPA.getEntityManagerFactory().createEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<D> q = cb.createQuery(clazz);
            Root<D> from = q.from(clazz);
            List<Predicate> predicates = new ArrayList<>();
            for(String paramName : params.keySet()) {
                Predicate p = cb.equal(from.get(paramName), params.get(paramName));
                predicates.add(p);
            }
            Predicate[] arr = new Predicate[predicates.size()];
            arr = predicates.toArray(arr);
            q.select(from).where(cb.and(arr));
            return em.createQuery(q).getResultList();
        } finally {
            em.close();
        }
    }

    public void update(D doObject) {
        EntityManager em = JPA.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(doObject);
            em.getTransaction().commit();
        } catch (Throwable t){
            em.getTransaction().rollback();
            LOGGER.error("JPA error during save ", t);
            throw t;
        } finally {
            em.close();
        }
    }
    public void delete(D doObject) {
        EntityManager em = JPA.getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            D managed = em.merge(doObject);
//            em.refresh(managed);
            em.remove(managed);
            em.getTransaction().commit();
        } catch (Throwable t){
            em.getTransaction().rollback();
            LOGGER.error("JPA error during save ", t);
            throw t;
        } finally {
            em.close();
        }
    }
}
