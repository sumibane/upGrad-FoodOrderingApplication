package com.upgrad.FoodOrderingApp.api.controller;

import com.upgrad.FoodOrderingApp.api.model.*;

import com.upgrad.FoodOrderingApp.service.businness.*;
import com.upgrad.FoodOrderingApp.service.entity.*;
import com.upgrad.FoodOrderingApp.service.exception.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.sql.Timestamp;;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@CrossOrigin(allowedHeaders = "*", origins = "*", exposedHeaders = ("access-token"))

@RestController("/")

public class OrderController {
    @Autowired
    OrderService orderService;

    @Autowired
    CustomerService customerService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    AddressService addressService;

    @Autowired
    ItemService itemService;

    @Autowired
    RestaurantService restaurantService;

    /* The method handles get Coupon By CouponName request.It takes authorization from the header and coupon name as the path vataible.
    & produces response in CouponDetailsResponse and returns UUID,Coupon Name and Percentage of coupon present in the DB and if error returns error code and error Message.
    */
    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, path = "order/coupon/{coupon_name}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CouponDetailsResponse> getCouponByCouponName(@RequestHeader(value = "authorization") final String authorization, @PathVariable(value = "coupon_name") final String couponName) throws AuthorizationFailedException, CouponNotFoundException {
        String[] accessToken = authorization.split("Bearer ");
        if (accessToken.length < 2) {


            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in");
        }


        CustomerEntity customerEntity = customerService.getCustomer(accessToken[1]);
        CouponEntity couponEntity = orderService.getCouponByCouponName(couponName);

        CouponDetailsResponse couponDetailsResponse = new CouponDetailsResponse()
                .couponName(couponEntity.getCouponName())
                .id(UUID.fromString(couponEntity.getUuid()))
                .percent(couponEntity.getPercent());

        return new ResponseEntity<CouponDetailsResponse>(couponDetailsResponse, HttpStatus.OK);


    }



    /* The method handles past order request of customer.It takes authorization from the header
    & produces response in CustomerOrderResponse and returns details of all the past order arranged in date wise and if error returns error code and error Message.
    */

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, path = "/order", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CustomerOrderResponse> getPastOrderOfUser(@RequestHeader(value = "authorization") final String authorization) throws AuthorizationFailedException {
        String[] accessToken = authorization.split("Bearer ");
        if (accessToken.length < 2) {

            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in");
        }
        CustomerEntity customerEntity = customerService.getCustomer(accessToken[1]);
        //Calls getOrdersByCustomers of orderService to get all the past orders of the customer.

        List<OrderEntity> ordersEntities = orderService.getOrdersByCustomers(customerEntity.getUuid());


        //Creating List of OrderList
        List<OrderList> orderLists = new LinkedList<>();


        if (ordersEntities != null) {  //Checking if orderentities is null if yes them empty list is returned
            for (OrderEntity orderEntity : ordersEntities) {

                //Calls getOrderItemsByOrder by order of orderService get all the items ordered in past by orders.
                List<OrderItemEntity> orderItemEntities = orderService.getOrderItemsByOrder(orderEntity);
                //Creating ItemQuantitiesResponse List
                List<ItemQuantityResponse> itemQuantityResponseList = new LinkedList<>();
                orderItemEntities.forEach(orderItemEntity -> {
                    //Creating new ItemQuantityResponseItem
                    ItemQuantityResponseItem itemQuantityResponseItem = new ItemQuantityResponseItem()

                            .itemName(orderItemEntity.getItem().getItemName())
                            .itemPrice(orderItemEntity.getItem().getPrice())
                            .id(UUID.fromString(orderItemEntity.getItem().getUuid()))
                            .type(ItemQuantityResponseItem.TypeEnum.valueOf(orderItemEntity.getItem().getType().toString()));

                    //Creating ItemQuantityResponse which will be added to the list
                    ItemQuantityResponse itemQuantityResponse = new ItemQuantityResponse()
                            .item(itemQuantityResponseItem)
                            .quantity(orderItemEntity.getQuantity())
                            .price(orderItemEntity.getPrice());

                    itemQuantityResponseList.add(itemQuantityResponse);
                });
                //Creating OrderListAddressState to add in the address
                OrderListAddressState orderListAddressState = new OrderListAddressState()
                        .id(UUID.fromString(orderEntity.getAddress().getState().getUuid()))
                        .stateName(orderEntity.getAddress().getState().getStateName());

                //Creating OrderListAddress to add address to the orderList
                OrderListAddress orderListAddress = new OrderListAddress()
                        .id(UUID.fromString(orderEntity.getAddress().getUuid()))
                        .flatBuildingName(orderEntity.getAddress().getFlatBuilNo())
                        .locality(orderEntity.getAddress().getLocality())
                        .city(orderEntity.getAddress().getCity())
                        .pincode(orderEntity.getAddress().getPincode())
                        .state(orderListAddressState);
                //Creating OrderListCoupon to add Coupon to the orderList
                OrderListCoupon orderListCoupon = new OrderListCoupon()
                        .couponName(orderEntity.getCoupon().getCouponName())
                        .id(UUID.fromString(orderEntity.getCoupon().getUuid()))
                        .percent(orderEntity.getCoupon().getPercent());

                //Creating OrderListCustomer to add Customer to the orderList
                OrderListCustomer orderListCustomer = new OrderListCustomer()
                        .id(UUID.fromString(orderEntity.getCustomer().getUuid()))
                        .firstName(orderEntity.getCustomer().getFirstName())
                        .lastName(orderEntity.getCustomer().getLastName())
                        .emailAddress(orderEntity.getCustomer().getEmail())
                        .contactNumber(orderEntity.getCustomer().getContactNumber());

                //Creating OrderListPayment to add Payment to the orderList
                OrderListPayment orderListPayment = new OrderListPayment()
                        .id(UUID.fromString(orderEntity.getPayment().getUuid()))
                        .paymentName(orderEntity.getPayment().getPaymentName());


                //Craeting orderList to add all the above info and then add it orderLists to finally add it to CustomerOrderResponse
                OrderList orderList = new OrderList()
                        .id(UUID.fromString(orderEntity.getUuid()))
                        .itemQuantities(itemQuantityResponseList)
                        .address(orderListAddress)
                        .bill(BigDecimal.valueOf(orderEntity.getBill()))
                        .date(String.valueOf(orderEntity.getDate()))
                        .discount(BigDecimal.valueOf(orderEntity.getDiscount()))
                        .coupon(orderListCoupon)
                        .customer(orderListCustomer)
                        .payment(orderListPayment);
                orderLists.add(orderList);

            }
            CustomerOrderResponse customerOrderResponse = new CustomerOrderResponse()
                    .orders(orderLists);

            return new ResponseEntity<CustomerOrderResponse>(customerOrderResponse, HttpStatus.OK);
        } else {
            return new ResponseEntity<CustomerOrderResponse>(new CustomerOrderResponse(), HttpStatus.OK);//If no order created by customer empty array is returned.
        }
    }


     /* The method handles save Order request.It takes authorization from the header and other details in SaveOrderRequest.
    & produces response in SaveOrderResponse and returns UUID and successful message and if error returns error code and error Message.
    */

   /* @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, path = "/order", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SaveOrderResponse> saveOrder(@RequestHeader(value = "authorization") final String authorization, @RequestBody(required = false) final SaveOrderRequest saveOrderRequest) throws AuthorizationFailedException, CouponNotFoundException, PaymentMethodNotFoundException, AddressNotFoundException, ItemNotFoundException, RestaurantNotFoundException {
        String[] accessToken = authorization.split("Bearer ");
        if (accessToken.length < 2) {
            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in");
        }
        CustomerEntity customerEntity = customerService.getCustomer(accessToken[1]);

        //Calls orderService getCouponByCouponId method to get the CouponEntity by it uuid.
        CouponEntity couponEntity = orderService.getCouponByCouponId(saveOrderRequest.getCouponId().toString());

        //Calls paymentService getPaymentByUUID method to get the PaymentEntity by it uuid.
        PaymentEntity paymentEntity = paymentService.getPaymentByUUID(saveOrderRequest.getPaymentId().toString());

        //Calls addressService getAddressByUUID method to get the AddressEntity by it uuid.
        AddressEntity addressEntity = addressService.getAddressByUUID(saveOrderRequest.getAddressId(), customerEntity);

        //Calls restaurantService restaurantByUUID method to get the RestaurantEntity by it uuid.
        RestaurantEntity restaurantEntity = restaurantService.restaurantByUUID(saveOrderRequest.getRestaurantId().toString());

        //Creating new order entity from the details fetched earlier and request details received.
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUuid(UUID.randomUUID().toString());
        orderEntity.setBill(saveOrderRequest.getBill().floatValue());
        orderEntity.setDate(timestamp);
        orderEntity.setCustomer(customerEntity);
        orderEntity.setDiscount(saveOrderRequest.getDiscount().doubleValue());
        orderEntity.setPayment(paymentEntity);
        orderEntity.setAddress(addressEntity);
        orderEntity.setRestaurant(restaurantEntity);
        orderEntity.setCoupon(couponEntity);

        //Calls orderService saveOrder method to persist the order in database.
        OrderEntity savedOrderEntity = orderService.saveOrder(orderEntity);

        //Setting items for the OrderItemEntity
        List<ItemQuantity> itemQuantities = saveOrderRequest.getItemQuantities();
        for (ItemQuantity itemQuantity : itemQuantities) {

            OrderItemEntity orderItemEntity = new OrderItemEntity();

            ItemEntity itemEntity = itemService.getItemByUUID(itemQuantity.getItemId().toString());

            orderItemEntity.setItem(itemEntity);
            orderItemEntity.setOrder(orderEntity);
            orderItemEntity.setPrice(itemQuantity.getPrice());
            orderItemEntity.setQuantity(itemQuantity.getQuantity());

            //calls orderService saveOrderItem to persist the orderItemEntity cretaed
            OrderItemEntity savedOrderItem = orderService.saveOrderItem(orderItemEntity);

        }
        //Creating the SaveOrderResponse for the endpoint containing UUID and success message.
        SaveOrderResponse saveOrderResponse = new SaveOrderResponse()
                .id(savedOrderEntity.getUuid())
                .status("ORDER SUCCESSFULLY PLACED");
        return new ResponseEntity<SaveOrderResponse>(saveOrderResponse, HttpStatus.CREATED);

    }*/

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, path = "/order", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SaveOrderResponse> saveOrder(
            @RequestBody(required = false) final SaveOrderRequest saveOrderRequest,
            @RequestHeader("authorization") final String authorization)
            throws AuthorizationFailedException, CouponNotFoundException,
            AddressNotFoundException, PaymentMethodNotFoundException,
            RestaurantNotFoundException, ItemNotFoundException
    {
        String accessToken = authorization.split("Bearer ")[1];
        CustomerEntity customerEntity = customerService.getCustomer(accessToken);

        final OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUuid(UUID.randomUUID().toString());
        orderEntity.setCoupon(orderService.getCouponByCouponId(saveOrderRequest.getCouponId().toString()));
        orderEntity.setPayment(paymentService.getPaymentByUUID(saveOrderRequest.getPaymentId().toString()));
        orderEntity.setCustomer(customerEntity);
        orderEntity.setAddress(addressService.getAddressByUUID(saveOrderRequest.getAddressId(), customerEntity));
        orderEntity.setBill(saveOrderRequest.getBill().doubleValue());
        orderEntity.setDiscount(saveOrderRequest.getDiscount().doubleValue());
        orderEntity.setRestaurant(restaurantService.restaurantByUUID(saveOrderRequest.getRestaurantId().toString()));
        orderEntity.setDate(new Date());
        OrderEntity savedOrderEntity = orderService.saveOrder(orderEntity);

        for (ItemQuantity itemQuantity : saveOrderRequest.getItemQuantities()) {
            OrderItemEntity orderItemEntity = new OrderItemEntity();
            orderItemEntity.setOrder(savedOrderEntity);
            orderItemEntity.setItem(itemService.getItemByUUID(itemQuantity.getItemId().toString()));
            orderItemEntity.setQuantity(itemQuantity.getQuantity());
            orderItemEntity.setPrice(itemQuantity.getPrice());
            orderService.saveOrderItem(orderItemEntity);
        }

        SaveOrderResponse saveOrderResponse = new SaveOrderResponse()
                .id(savedOrderEntity.getUuid()).status("ORDER SUCCESSFULLY PLACED");
        return new ResponseEntity<SaveOrderResponse>(saveOrderResponse, HttpStatus.CREATED);
    }
}

