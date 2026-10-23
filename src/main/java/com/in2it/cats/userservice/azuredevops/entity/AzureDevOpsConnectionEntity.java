
package com.in2it.cats.userservice.azuredevops.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "azure_devops_connections")
public class AzureDevOpsConnectionEntity {

    @Id
    private String id;
    private String connectionName;
    private String organizationName;
    private String projectName;
    private String encryptedPersonalAccessToken;
    private String status;
    private LocalDateTime createdAt;
}
