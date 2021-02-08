package com.upgrad.FoodOrderingApp.service.dao;

import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.entity.OrderEntity;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class OrdersDao {
    @PersistenceContext
    private EntityManager entityManager;

    //To get List of order from the db Corresponding to Customers
    public List<OrderEntity>getOrdersByCustomers(CustomerEntity customerEntity){
        try {
            return entityManager.createNamedQuery("getOrdersByCustomers", OrderEntity.class).setParameter("customer",customerEntity).getResultList();
        }catch (NoResultException e){
            return null;
        }

    }

    //To save Order in the db
    public OrderEntity saveOrder(OrderEntity orderEntity){
        entityManager.persist(orderEntity);
        return orderEntity;
    }

}
