
package com.in2it.cats.userservice.azuredevops.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureDevOpsRepositoryResponseDTO {

    private String repositoryId;
    private String repositoryName;
    private String projectName;
    private String remoteUrl;
    private String defaultBranch;
}
