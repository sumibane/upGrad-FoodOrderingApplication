package com.upgrad.FoodOrderingApp.service.businness;

import com.upgrad.FoodOrderingApp.service.dao.CustomerAuthDao;
import com.upgrad.FoodOrderingApp.service.dao.CustomerDao;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import com.upgrad.FoodOrderingApp.service.exception.UpdateCustomerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class CustomerService {

    @Autowired
    private CustomerDao customer_dao;

    @Autowired
    private CustomerAuthDao customer_auth_dao;

    @Autowired
    private PasswordCryptographyProvider pwd_crypt;


    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity saveCustomer(CustomerEntity customerEntity) throws SignUpRestrictedException {

        CustomerEntity existed_customer = customer_dao.getUserByContactNumber(customerEntity.getContactNumber());
        // customer details validation part
        if (existed_customer != null) {
            throw new SignUpRestrictedException("SGR-001", "This contact number is already registered! Try other contact number.");
        }

        if (!customerEntity.getEmail().matches("[a-zA-Z0-9]{3,}@[a-zA-Z0-9]{2,}\\.[a-zA-Z0-9]{2,}")) {
            throw new SignUpRestrictedException("SGR-002", "Invalid email-id format!");
        }

        if (!customerEntity.getContactNumber().matches("[0-9]{10,}")) {
            throw new SignUpRestrictedException("SGR-003", "Invalid contact number!");
        }

        if (!isPasswordStrong(customerEntity.getPassword())) {
            throw new SignUpRestrictedException("SGR-004", "Weak password!");
        }

        String[] encryptedText = pwd_crypt.encrypt(customerEntity.getPassword());
        customerEntity.setSalt(encryptedText[0]);
        customerEntity.setPassword(encryptedText[1]);

        try {
            return customer_dao.createCustomer(customerEntity);
        } catch (Exception e) {
            throw new SignUpRestrictedException("SGR-000", "Unknown database error while creating customer");
        }

    }

    private boolean isPasswordStrong(String password) {
        //min 8 char long, 1 digit, 1 uppercase letter, 1 special character in [#@$%&*!^], total of 8 characters
        boolean containdigit = false;
        boolean containUpperCaseChar = false;
        boolean containSpecialChar = false;

        if (password.length() < 8)
            return false;
        char c;

        for (int i = 0; i < password.length(); i++) {

            c = password.charAt(i);

            if (c >= '0' && c <= '9') {
                containdigit = true;
            } else if (c >= 'A' && c <= 'Z') {
                containUpperCaseChar = true;
            } else if (c == '#' || c == '@' || c == '$' || c == '%' || c == '&' || c == '*' || c == '!' || c == '^') {
                containSpecialChar = true;
            }

            if (containdigit & containUpperCaseChar & containSpecialChar) {
                return true;
            }
        }

        return false;
    }

    public CustomerAuthEntity authenticate(String contactNumber, String password) throws AuthenticationFailedException {

        if (contactNumber == null ||
                contactNumber.isEmpty() ||
                password == null ||
                password.isEmpty()) {
            throw new AuthenticationFailedException("ATH-003", "Incorrect format of decoded customer name and password");
        }

        CustomerEntity customerEntity = customer_dao.getUserByContactNumber(contactNumber);
        if (customerEntity == null) {
            throw new AuthenticationFailedException("ATH-001", "This contact number has not been registered!");
        }

        String encryptedPassword = PasswordCryptographyProvider.encrypt(password, customerEntity.getSalt());
        if (!encryptedPassword.equals(customerEntity.getPassword())) {
            throw new AuthenticationFailedException("ATH-002", "Invalid Credentials");
        }

        CustomerAuthEntity customerAuthEntity = new CustomerAuthEntity();
        customerAuthEntity.setCustomer(customerEntity);

        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime expiresAt = now.plusHours(8);

        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(encryptedPassword);

        customerAuthEntity.setAccessToken(jwtTokenProvider.generateToken(customerEntity.getUuid(), now, expiresAt));
        customerAuthEntity.setLoginAt(now);
        customerAuthEntity.setExpiresAt(expiresAt);
        customerAuthEntity.setUuid(UUID.randomUUID().toString());

        customer_auth_dao.createAuthToken(customerAuthEntity);

        return customerAuthEntity;
    }

    public CustomerEntity getCustomer(final String accessToken) throws AuthorizationFailedException {
        return getCustomerAuth(accessToken).getCustomer();
    }

    private CustomerAuthEntity getCustomerAuth(final String accessToken) throws AuthorizationFailedException {
        CustomerAuthEntity customerAuthEntity = customer_auth_dao.getAuthTokenEntityByAccessToken(accessToken);
        if (customerAuthEntity != null) {
            // Token exist but customer logged out already or token expired
            if (customerAuthEntity.getLogoutAt() != null) {
                throw new AuthorizationFailedException("ATHR-002", "Customer is logged out. Log in again to access this endpoint.");
            } else if (customerAuthEntity.getExpiresAt().compareTo(ZonedDateTime.now()) <= 0) {
                throw new AuthorizationFailedException("ATHR-003", "Your session is expired. Log in again to access this endpoint.");
            }
            return customerAuthEntity;
        } else {
            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerAuthEntity logout(final String accessToken) throws AuthorizationFailedException {

        CustomerAuthEntity customerAuthEntity = getCustomerAuth(accessToken);

        customerAuthEntity.setLogoutAt(ZonedDateTime.now());

        return customer_auth_dao.updateAuthToken(customerAuthEntity);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity updateCustomer(CustomerEntity customerEntity) throws AuthorizationFailedException {
        return customer_dao.updateCustomer(customerEntity);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerEntity updateCustomerPassword(String oldPassword, String newPassword, CustomerEntity customerEntity) throws UpdateCustomerException {

        if (!isPasswordStrong(newPassword)) {
            throw new UpdateCustomerException("UCR-001", "Weak password!");
        }

        String encryptedOldPassword = PasswordCryptographyProvider.encrypt(oldPassword, customerEntity.getSalt());
        if (!encryptedOldPassword.equals(customerEntity.getPassword())) {
            throw new UpdateCustomerException("UCR-004", "Incorrect old password!");
        }
        customerEntity.setPassword(PasswordCryptographyProvider.encrypt(newPassword, customerEntity.getSalt()));
        return customer_dao.updateCustomer(customerEntity);
    }
}
