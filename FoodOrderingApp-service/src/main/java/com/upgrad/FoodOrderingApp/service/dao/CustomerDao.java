package com.upgrad.FoodOrderingApp.service.dao;

import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import org.springframework.stereotype.Repository;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Repository
public class CustomerDao {

    @PersistenceContext
    private EntityManager em;

    public CustomerEntity createCustomer(CustomerEntity customerEntity) {
        try {
            em.persist(customerEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customerEntity;
    }

    public CustomerEntity getUserByContactNumber(String contactNumber) {
        try {
            return em.createNamedQuery("customerByContactNumber", CustomerEntity.class).setParameter("contactNumber", contactNumber).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public CustomerEntity updateCustomer(final CustomerEntity updateCustomerEntity) {
        try {
            em.merge(updateCustomerEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updateCustomerEntity;
    }


    public CustomerEntity getCustomerByUuid(final String uuid){
        try{

            return em.createNamedQuery("customerByUuid",CustomerEntity.class).setParameter("uuid",uuid).getSingleResult();

        }catch (NoResultException e){
            return null;
        }
    }


}

