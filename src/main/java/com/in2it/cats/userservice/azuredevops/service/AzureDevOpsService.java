
package com.in2it.cats.userservice.azuredevops.service;

import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsConnectionRequestDTO;
import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsConnectionResponseDTO;
import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsRepositoryResponseDTO;

import java.util.List;

public interface AzureDevOpsService {

    String testConnection(AzureDevOpsConnectionRequestDTO requestDTO);
    List<AzureDevOpsRepositoryResponseDTO> getRepositories(AzureDevOpsConnectionRequestDTO requestDTO);
    String saveConnection(AzureDevOpsConnectionRequestDTO requestDTO);
    List<AzureDevOpsConnectionResponseDTO> getConnections();
    List<AzureDevOpsRepositoryResponseDTO> getRepositoriesByConnectionId(String connectionId);
}
