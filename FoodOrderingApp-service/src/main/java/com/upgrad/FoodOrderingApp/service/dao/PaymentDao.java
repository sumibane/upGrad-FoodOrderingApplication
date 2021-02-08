package com.upgrad.FoodOrderingApp.service.dao;

import com.upgrad.FoodOrderingApp.service.entity.PaymentEntity;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

/*
 * This dao class access DB with respect to payment entity
 *  */
@Repository
public class PaymentDao {
    @PersistenceContext
    private EntityManager entityManager;

    /*
     * This method return all payment methods
     *  */

    public List<PaymentEntity> getAllPaymentMethods() {
        try {
            return entityManager.createNamedQuery("getAllPaymentMethods", PaymentEntity.class).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    //To get Payment By UUID from the db
    public PaymentEntity getPaymentByUUID(String paymentId) {
        try{
            PaymentEntity paymentEntity = entityManager.createNamedQuery("getPaymentByUUID",PaymentEntity.class).setParameter("uuid",paymentId).getSingleResult();
            return paymentEntity;
        }catch (NoResultException nre){
            return null;
        }
    }
}
