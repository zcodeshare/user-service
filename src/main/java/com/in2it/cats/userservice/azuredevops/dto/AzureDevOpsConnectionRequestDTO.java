
package com.in2it.cats.userservice.azuredevops.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureDevOpsConnectionRequestDTO {

    @NotBlank(message = "Connection name is required")
    private String connectionName;

    @NotBlank(message = "Organization name is required")
    private String organizationName;

    private String projectName;

    @NotBlank(message = "Personal access token is required")
    private String personalAccessToken;
}
