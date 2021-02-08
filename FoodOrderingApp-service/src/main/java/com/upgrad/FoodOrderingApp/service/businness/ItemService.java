package com.upgrad.FoodOrderingApp.service.businness;

import com.upgrad.FoodOrderingApp.service.dao.ItemDao;
import com.upgrad.FoodOrderingApp.service.entity.ItemEntity;

import com.upgrad.FoodOrderingApp.service.entity.RestaurantEntity;
import com.upgrad.FoodOrderingApp.service.exception.ItemNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemDao itemDao;

    public ItemEntity getItemsByUuid(String uuid) {
        return itemDao.getItemsByUuid(uuid);

    }

    public List<ItemEntity> getItemsByPopularity(RestaurantEntity restaurantEntity) {

        return itemDao.getItemsByPopularity(restaurantEntity.getId());

    }

    public List<ItemEntity> getItemsByCategoryAndRestaurant(String restaurantId, String categoryId) {
        return itemDao.getItemsByCategoryAndRestaurant(restaurantId, categoryId);
    }

    public ItemEntity getItemByUUID(String itemUuid) throws ItemNotFoundException {
        ItemEntity itemEntity = itemDao.getItemsByUuid(itemUuid);
        if(itemEntity == null){
            throw new ItemNotFoundException("INF-003","No item by this id exist");
        }
        return itemEntity;
    }
}

