package com.paulpladziewicz.fremontmi.repositories;

import com.paulpladziewicz.fremontmi.models.StripeTransactionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StripeTransactionRecordRepository extends MongoRepository<StripeTransactionRecord, String> {
}
