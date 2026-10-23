
package com.in2it.cats.userservice.azuredevops.controller;

import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsConnectionRequestDTO;
import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsConnectionResponseDTO;
import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsRepositoryResponseDTO;
import com.in2it.cats.userservice.azuredevops.service.AzureDevOpsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/azure-devops")
public class AzureDevOpsController {

    private final AzureDevOpsService azureDevOpsService;

    public AzureDevOpsController(AzureDevOpsService azureDevOpsService) {
        this.azureDevOpsService = azureDevOpsService;
    }

    @PostMapping("/test-connection")
    public ResponseEntity<String> testConnection(@Valid @RequestBody AzureDevOpsConnectionRequestDTO requestDTO) {

        String response = azureDevOpsService.testConnection(requestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/repositories")
    public ResponseEntity<List<AzureDevOpsRepositoryResponseDTO>> getRepositories(@Valid @RequestBody AzureDevOpsConnectionRequestDTO requestDTO) {

        List<AzureDevOpsRepositoryResponseDTO> repositories = azureDevOpsService.getRepositories(requestDTO);
        return ResponseEntity.ok(repositories);
    }

    @PostMapping("/connections")
    public ResponseEntity<String> saveConnection(@Valid @RequestBody AzureDevOpsConnectionRequestDTO requestDTO) {

        String response = azureDevOpsService.saveConnection(requestDTO);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/connections")
    public ResponseEntity<List<AzureDevOpsConnectionResponseDTO>> getConnections() {

        List<AzureDevOpsConnectionResponseDTO> response = azureDevOpsService.getConnections();
        return ResponseEntity.ok(response);
    }


    @GetMapping("/connections/{id}/repositories")
    public ResponseEntity<List<AzureDevOpsRepositoryResponseDTO>> getRepositoriesByConnectionId(
            @PathVariable String id) {

        List<AzureDevOpsRepositoryResponseDTO> repositories = azureDevOpsService.getRepositoriesByConnectionId(id);
        return ResponseEntity.ok(repositories);
    }


}
