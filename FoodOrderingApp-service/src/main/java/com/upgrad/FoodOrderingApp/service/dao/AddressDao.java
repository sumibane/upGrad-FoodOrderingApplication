package com.upgrad.FoodOrderingApp.service.dao;

import com.upgrad.FoodOrderingApp.service.entity.AddressEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAddressEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class AddressDao {

    @PersistenceContext
    private EntityManager em;

    public CustomerAddressEntity getAddressByUUID(String addressUuid, String customerUuid) {
        try {
            return em.createNamedQuery("getCustomerAddressByUUID", CustomerAddressEntity.class).setParameter("addressUuid", addressUuid).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<CustomerAddressEntity> getAllCustomerAddress(CustomerEntity customer) {
        try {
            return em.createNamedQuery("getAllCustomerAddress", CustomerAddressEntity.class).setParameter("customer", customer).getResultList();
        } catch (NoResultException nre) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public AddressEntity saveAddress(CustomerAddressEntity customerAddressEntity) {
        try {
            em.persist(customerAddressEntity.getAddress());
            em.persist(customerAddressEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customerAddressEntity.getAddress();
    }

    public AddressEntity deleteAddress(AddressEntity addressEntity) {
        try {
            em.remove(addressEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return addressEntity;
    }
}
