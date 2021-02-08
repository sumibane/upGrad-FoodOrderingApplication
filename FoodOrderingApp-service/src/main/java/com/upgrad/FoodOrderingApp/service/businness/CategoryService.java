package com.upgrad.FoodOrderingApp.service.businness;

import com.upgrad.FoodOrderingApp.service.dao.CategoryDao;

import com.upgrad.FoodOrderingApp.service.dao.RestaurantCategoryDao;
import com.upgrad.FoodOrderingApp.service.dao.RestaurantDao;
import com.upgrad.FoodOrderingApp.service.entity.*;

//import com.upgrad.FoodOrderingApp.service.entity.RestaurantCategoryEntity;
import com.upgrad.FoodOrderingApp.service.exception.CategoryNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryDao categoryDao;


    @Autowired
    private RestaurantDao restaurantDao;

    @Autowired
    private RestaurantCategoryDao restaurantCategoryDao;

    public List<CategoryEntity> getCategoriesByRestaurant(String restaurantId) {
        //Calls getRestaurantByUuid of restaurantDao to get RestaurantEntity
        RestaurantEntity restaurantEntity = restaurantDao.getRestaurantByUuid(restaurantId);

        //Calls getCategoriesByRestaurant of restaurantCategoryDao to get list of RestaurantCategoryEntity
        List<RestaurantCategoryEntity> restaurantCategoryEntities = restaurantCategoryDao.getCategoriesByRestaurant(restaurantEntity);

        //Creating the list of the Category entity to be returned.
        List<CategoryEntity> categoryEntities = new LinkedList<>();
        restaurantCategoryEntities.forEach(restaurantCategoryEntity -> {
            categoryEntities.add(restaurantCategoryEntity.getCategory());
        });
        return categoryEntities;
    }


    public CategoryEntity getCategoryById(String categoryId) throws CategoryNotFoundException {

        if (categoryId == null || categoryId.isEmpty()) {
            throw new CategoryNotFoundException("CNF-001", "Category id field should not be empty");
        }

        CategoryEntity categoryEntity = categoryDao.getCategoryById(categoryId);
        if (categoryEntity == null) {
            throw new CategoryNotFoundException("CNF-002", "No category by this id");
        }

        List<CategoryItemEntity> listCategoryItemEntity = categoryDao.getCategoryItemEntityByCategoryUuid(categoryId);
        //Disabling this code because Unit test are written differently
        //categoryEntity.setCategoryItem(listCategoryItemEntity);

       //  { TODO: Remove This code if Test CategoryControllerTest.java:shouldGetCategoryById() is fixed
        List<ItemEntity> listItemEntity = new ArrayList<>();
        for (CategoryItemEntity ci : listCategoryItemEntity) {
            listItemEntity.add(ci.getItem());
        }
        categoryEntity.setItems(listItemEntity);
        // }

        return categoryEntity;
    }

    public List<CategoryEntity> getAllCategoriesOrderedByName() {
        return categoryDao.getAllCategoriesOrderedByName();
    }
}
