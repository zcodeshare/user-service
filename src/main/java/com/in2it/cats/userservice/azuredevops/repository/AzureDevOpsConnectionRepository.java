
package com.in2it.cats.userservice.azuredevops.repository;

import com.in2it.cats.userservice.azuredevops.entity.AzureDevOpsConnectionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AzureDevOpsConnectionRepository extends MongoRepository<AzureDevOpsConnectionEntity, String> {

    Optional<AzureDevOpsConnectionEntity> findByConnectionName(String connectionName);
    boolean existsByConnectionName(String connectionName);
}
