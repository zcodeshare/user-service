
package com.in2it.cats.userservice.securityscan.repository;

import com.in2it.cats.userservice.securityscan.entity.SecurityScanEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SecurityScanRepository extends MongoRepository<SecurityScanEntity, String> {
}
