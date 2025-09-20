package com.paulpladziewicz.fremontmi.billing;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BillingRepository extends MongoRepository<StripeSubscriptionRecord, String> {

}

