
package com.in2it.cats.userservice.azuredevops.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AzureDevOpsConnectionResponseDTO {

    private String id;
    private String connectionName;
    private String organizationName;
    private String projectName;
    private String status;
    private LocalDateTime createdAt;
}
