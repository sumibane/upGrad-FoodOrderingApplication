package com.upgrad.FoodOrderingApp.api.controller;

import com.upgrad.FoodOrderingApp.api.common.Utility;
import com.upgrad.FoodOrderingApp.api.model.*;
import com.upgrad.FoodOrderingApp.service.businness.CustomerService;
import com.upgrad.FoodOrderingApp.service.businness.PasswordCryptographyProvider;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import com.upgrad.FoodOrderingApp.service.exception.UpdateCustomerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CrossOrigin(allowedHeaders = "*", origins = "*", exposedHeaders = ("access-token"))
@RestController
@RequestMapping("/")
public class CustomerController {

    @Autowired
    CustomerService customer_service;

    @Autowired
    private PasswordCryptographyProvider pwd_crypt;

    /*
     * This endpoint is used to signing up a new user in the FoodOrderingAppBackend.
     * input - contains body with details like
     * First Name, Last Name, Email id, password, contact number.
     * output - Success - SignupUserResponse containing created user detail with its uuid
     *          Failure - Failure Code
     */
    @RequestMapping(
            method = RequestMethod.POST,
            path = "/customer/signup",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SignupCustomerResponse> customerSignup(
            @RequestBody final SignupCustomerRequest signupCustomerRequest)
            throws SignUpRestrictedException {

        if (signupCustomerRequest.getFirstName() == null ||
                signupCustomerRequest.getFirstName().isEmpty() ||
                signupCustomerRequest.getPassword() == null ||
                signupCustomerRequest.getPassword().isEmpty() ||
                signupCustomerRequest.getEmailAddress() == null ||
                signupCustomerRequest.getEmailAddress().isEmpty() ||
                signupCustomerRequest.getContactNumber() == null ||
                signupCustomerRequest.getContactNumber().isEmpty()) {
            throw new SignUpRestrictedException("SGR-005", "Except last name all fields should be filled.");
        }

        //create an object for CustomerEntity
        CustomerEntity customerEntity = new CustomerEntity();

        //create a new random unique uuid and set it to new CustomerEntity
        customerEntity.setUuid(UUID.randomUUID().toString());

        //setting all the fields from the Request
        customerEntity.setFirstName(signupCustomerRequest.getFirstName());
        customerEntity.setLastName(signupCustomerRequest.getLastName());
        customerEntity.setPassword(signupCustomerRequest.getPassword());
        customerEntity.setEmail(signupCustomerRequest.getEmailAddress());
        customerEntity.setContactNumber(signupCustomerRequest.getContactNumber());

        //Call CustomerService to create a new customer Entity
        CustomerEntity ce = customer_service.saveCustomer(customerEntity);

        //creating response with customer uuid
        SignupCustomerResponse signupCustomerResponse = new SignupCustomerResponse().id(ce.getUuid()).status("CUSTOMER SUCCESSFULLY REGISTERED");
        return new ResponseEntity<SignupCustomerResponse>(signupCustomerResponse, HttpStatus.CREATED);
    }

    /*
     * This endpoint is used to login user in the FoodOrderingAppBackend.
     * input - authorization field containing Basic + Base64 Encoded String of "username(phonenumber):password"
     * output - Success - Auth Token with user uuid
     *          Failure - Failure Code
     */
    @RequestMapping(
            method = RequestMethod.POST,
            path = "/customer/login",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LoginResponse> login(
            @RequestHeader("authorization") final String authorization)
            throws AuthenticationFailedException, AuthorizationFailedException {

        //split and extract authorization base 64 code string from "authorization" field
        String[] base64EncodedString = authorization.split("Basic ");

        //decode base64 string from "authorization" field
        if (base64EncodedString.length != 2) {
            throw new AuthenticationFailedException("ATH-003", "Incorrect format of decoded customer name and password");
        }
        byte[] decodedArray = PasswordCryptographyProvider.getBase64DecodedStringAsBytes(base64EncodedString[1]);

        String decodedString = new String(decodedArray);

        //decoded string containing username(phonenumber) and password separated by ":"
        String[] decodedUserNamePassword = decodedString.split(":");

        if (decodedUserNamePassword.length != 2) {
            throw new AuthenticationFailedException("ATH-003", "Incorrect format of decoded customer name and password");
        }

        //get CustomerEntity from Authentication Token
        CustomerAuthEntity customerAuthEntity = customer_service.authenticate(decodedUserNamePassword[0], decodedUserNamePassword[1]);

        //send response with customer uuid and access token in HttpHeader
        LoginResponse loginResponse = new LoginResponse()
                .id(customerAuthEntity.getCustomer().getUuid())
                .firstName(customerAuthEntity.getCustomer().getFirstName())
                .lastName(customerAuthEntity.getCustomer().getLastName())
                .contactNumber(customerAuthEntity.getCustomer().getContactNumber())
                .emailAddress(customerAuthEntity.getCustomer().getEmail())
                .message("SIGNED IN SUCCESSFULLY");
        HttpHeaders headers = new HttpHeaders();
        headers.add("access-token", customerAuthEntity.getAccessToken());

        List<String> header = new ArrayList<>();
        header.add("access-token");
        headers.setAccessControlExposeHeaders(header);
        return new ResponseEntity<LoginResponse>(loginResponse, headers, HttpStatus.OK);
    }

    /*
     * This endpoint is used to logout user from the FoodOrderingAppBackend.
     * input - Bearer + authorization field containing access token generated from user sign-in
     * output - Success - with message
     *          Failure - Failure Code
     */
    @RequestMapping(
            method = RequestMethod.POST,
            path = "/customer/logout",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("authorization") final String authorization)
            throws AuthorizationFailedException {

        CustomerAuthEntity customer_auth_entity = customer_service.logout(Utility.getTokenFromAuthorizationField(authorization));

        //create response with logoutResponse customer uuid
        LogoutResponse logoutResponse = new LogoutResponse().id(customer_auth_entity.getCustomer().getUuid()).message("SIGNED OUT SUCCESSFULLY");
        HttpHeaders headers = new HttpHeaders();
        return new ResponseEntity<LogoutResponse>(logoutResponse, headers, HttpStatus.OK);

    }

    /*
     * This endpoint is used to update the user details for FoodOrderingAppBackend.
     * input - Bearer + authorization field containing access token generated from user sign-in as header
     * and the body containing details like first name and last name
     * output - Success - with message
     *          Failure - Failure Code
     */
    @RequestMapping(
            method = RequestMethod.PUT,
            path = "/customer",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<UpdateCustomerResponse> updateCustomer(
            @RequestHeader("authorization") final String authorization,
            @RequestBody final UpdateCustomerRequest updateCustomerRequest)
            throws UpdateCustomerException, AuthorizationFailedException {

        if (updateCustomerRequest.getFirstName() == null ||
                updateCustomerRequest.getFirstName().isEmpty()) {
            throw new UpdateCustomerException("UCR-002", "First name field should not be empty");
        }

        // Call authenticationService with access token came in authorization field.
        CustomerEntity customer_entity = customer_service.getCustomer(Utility.getTokenFromAuthorizationField(authorization));

        customer_entity.setFirstName(updateCustomerRequest.getFirstName());

        if (updateCustomerRequest.getLastName() != null &&
                !updateCustomerRequest.getLastName().isEmpty()) {
            customer_entity.setLastName(updateCustomerRequest.getLastName());
        }

        CustomerEntity updatedcustomerEntity = customer_service.updateCustomer(customer_entity);

        //create response with create customer uuid
        UpdateCustomerResponse updateCustomerResponse = new UpdateCustomerResponse()
                .id(updatedcustomerEntity.getUuid())
                .firstName(updatedcustomerEntity.getFirstName())
                .lastName(updatedcustomerEntity.getLastName())
                .status("CUSTOMER DETAILS UPDATED SUCCESSFULLY");

        return new ResponseEntity<UpdateCustomerResponse>(updateCustomerResponse, HttpStatus.OK);
    }

    /*
     * This endpoint is used to change the user password for the FoodOrderingAppBackend.
     * input - Bearer + authorization field containing access token generated from user sign-in as header
     * and the body containing details like old password and new password
     * output - Success - with message
     *          Failure - Failure Code
     */
    @RequestMapping(
            method = RequestMethod.PUT,
            path = "/customer/password",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<UpdatePasswordResponse> updatePassword(
            @RequestHeader("authorization") final String authorization,
            @RequestBody final UpdatePasswordRequest updatePasswordRequest)
            throws UpdateCustomerException, AuthorizationFailedException {

        if (updatePasswordRequest.getOldPassword() == null ||
                updatePasswordRequest.getOldPassword().isEmpty() ||
                updatePasswordRequest.getNewPassword() == null ||
                updatePasswordRequest.getNewPassword().isEmpty()) {
            throw new UpdateCustomerException("UCR-003", "No field should be empty");
        }

        // Call authenticationService with access token came in authorization field.
        CustomerEntity customer_entity = customer_service.getCustomer(Utility.getTokenFromAuthorizationField(authorization));

        customer_service.updateCustomerPassword(updatePasswordRequest.getOldPassword(), updatePasswordRequest.getNewPassword(), customer_entity);

        //create response with create customer uuid
        UpdatePasswordResponse updatePasswordResponse = new UpdatePasswordResponse().id(customer_entity.getUuid()).status("CUSTOMER PASSWORD UPDATED SUCCESSFULLY");

        return new ResponseEntity<UpdatePasswordResponse>(updatePasswordResponse, HttpStatus.OK);
    }
}
