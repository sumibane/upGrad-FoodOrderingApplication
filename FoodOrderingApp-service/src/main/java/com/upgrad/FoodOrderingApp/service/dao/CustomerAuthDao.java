package com.upgrad.FoodOrderingApp.service.dao;

import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Repository
public class CustomerAuthDao {


    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRED)

    public CustomerAuthEntity createAuthToken(final CustomerAuthEntity customerAuthEntity) {
        try {
            em.persist(customerAuthEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customerAuthEntity;
    }

    public CustomerAuthEntity updateAuthToken(final CustomerAuthEntity customerAuthEntity) {
        try {
            em.merge(customerAuthEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customerAuthEntity;
    }

    public CustomerAuthEntity getAuthTokenEntityByAccessToken(final String accessToken) {
        try {
            return em.createNamedQuery("customerAuthTokenByAccessToken", CustomerAuthEntity.class).setParameter("accessToken", accessToken).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
