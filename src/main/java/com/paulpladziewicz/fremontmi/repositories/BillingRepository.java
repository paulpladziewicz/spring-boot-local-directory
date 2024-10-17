package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.StripeSubscriptionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BillingRepository extends MongoRepository<StripeSubscriptionRecord, String> {

}

