package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Business;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.repositories.BusinessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BusinessService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessService.class);

    private final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public ServiceResponse<Business> createBusiness(Business business) {
        try {
            return ServiceResponse.value(businessRepository.save(business));
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to create a business", e);
            return ServiceResponse.error("database_access_exception");
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a business", e);
            return ServiceResponse.error("unexpected_error");
        }
    }

    public Optional<Business> findBusinessById(String businessId) {
        try {
            return businessRepository.findById(businessId);
        } catch (DataAccessException e) {
            logger.error("Database access error when trying to create a business", e);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Unexpected error when trying to create a business", e);
            return Optional.empty();
        }
    }
}
