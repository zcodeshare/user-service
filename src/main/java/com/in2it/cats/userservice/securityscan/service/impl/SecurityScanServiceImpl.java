
package com.in2it.cats.userservice.securityscan.service.impl;

import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsRepositoryResponseDTO;
import com.in2it.cats.userservice.azuredevops.service.AzureDevOpsService;
import com.in2it.cats.userservice.securityscan.dto.SecurityScanRequestDTO;
import com.in2it.cats.userservice.securityscan.dto.SecurityScanResponseDTO;
import com.in2it.cats.userservice.securityscan.entity.SecurityScanEntity;
import com.in2it.cats.userservice.securityscan.repository.SecurityScanRepository;
import com.in2it.cats.userservice.securityscan.service.RepositorySourceService;
import com.in2it.cats.userservice.securityscan.service.ScannerExecutionService;
import com.in2it.cats.userservice.securityscan.service.SecurityScanService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityScanServiceImpl implements SecurityScanService {

    private final AzureDevOpsService azureDevOpsService;
    private final SecurityScanRepository securityScanRepository;
    private final RepositorySourceService repositorySourceService;
    private final ScannerExecutionService scannerExecutionService;

    public SecurityScanServiceImpl(
            AzureDevOpsService azureDevOpsService,
            SecurityScanRepository securityScanRepository,
            RepositorySourceService repositorySourceService,
            ScannerExecutionService scannerExecutionService) {

        this.azureDevOpsService = azureDevOpsService;
        this.securityScanRepository = securityScanRepository;
        this.repositorySourceService = repositorySourceService;
        this.scannerExecutionService = scannerExecutionService;
    }

    @Override
    public SecurityScanResponseDTO startScan(SecurityScanRequestDTO requestDTO) {

        String scanType = requestDTO.getScanType().trim().toUpperCase();

        if (!scanType.equals("SAST") && !scanType.equals("SCA") && !scanType.equals("BOTH")) {

            throw new IllegalArgumentException("Scan type must be SAST, SCA, or BOTH");
        }

        List<AzureDevOpsRepositoryResponseDTO> repositories = azureDevOpsService.getRepositoriesByConnectionId(
                        requestDTO.getConnectionId());

        AzureDevOpsRepositoryResponseDTO selectedRepository =
                repositories.stream()
                        .filter(repository ->
                                repository.getRepositoryId() != null
                                        && repository.getRepositoryId().equals(
                                        requestDTO.getRepositoryId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Repository not found for this connection"));

        SecurityScanEntity scanEntity = new SecurityScanEntity();
        scanEntity.setConnectionId(requestDTO.getConnectionId());
        scanEntity.setRepositoryId(selectedRepository.getRepositoryId());
        scanEntity.setRepositoryName(selectedRepository.getRepositoryName());
        scanEntity.setScanType(scanType);
        scanEntity.setStatus("PENDING");
        scanEntity.setMessage("Scan request created");
        scanEntity.setCreatedAt(LocalDateTime.now());

        SecurityScanEntity savedScan = securityScanRepository.save(scanEntity);

        Path sourcePath = null;

        try {
            savedScan.setStatus("RUNNING");
            savedScan.setMessage("Downloading repository");
            savedScan.setStartedAt(LocalDateTime.now());
            savedScan = securityScanRepository.save(savedScan);

            sourcePath = repositorySourceService.downloadRepository(requestDTO.getConnectionId(), requestDTO.getRepositoryId());

            if (scanType.equals("SAST") || scanType.equals("BOTH")) {
                savedScan.setMessage("Running SAST scan");
                savedScan = securityScanRepository.save(savedScan);

                String sastResult = scannerExecutionService.runSast(sourcePath);

                savedScan.setSastResult(sastResult);
            }

            if (scanType.equals("SCA") || scanType.equals("BOTH")) {
                savedScan.setMessage("Running SCA scan");
                savedScan = securityScanRepository.save(savedScan);

                String scaResult = scannerExecutionService.runSca(sourcePath);

                savedScan.setScaResult(scaResult);
            }

            savedScan.setStatus("COMPLETED");
            savedScan.setMessage("Security scan completed successfully");

        } catch (Exception exception) {
            savedScan.setStatus("FAILED");
            savedScan.setMessage("Security scan failed: "
                    + exception.getMessage());

        } finally {
            savedScan.setCompletedAt(LocalDateTime.now());

            try {
                if (sourcePath != null) {
                    repositorySourceService.cleanupRepository(sourcePath);
                }
            } catch (Exception cleanupException) {
                savedScan.setMessage(savedScan.getMessage() + ". Warning: temporary files could not be cleaned up.");
            }

            savedScan = securityScanRepository.save(savedScan);
        }

        return toResponseDTO(savedScan);
    }

    private SecurityScanResponseDTO toResponseDTO(SecurityScanEntity scanEntity) {

        SecurityScanResponseDTO responseDTO = new SecurityScanResponseDTO();

        responseDTO.setScanId(scanEntity.getId());
        responseDTO.setConnectionId(scanEntity.getConnectionId());
        responseDTO.setRepositoryId(scanEntity.getRepositoryId());
        responseDTO.setRepositoryName(scanEntity.getRepositoryName());
        responseDTO.setScanType(scanEntity.getScanType());
        responseDTO.setStatus(scanEntity.getStatus());
        responseDTO.setMessage(scanEntity.getMessage());
        responseDTO.setCreatedAt(scanEntity.getCreatedAt());
        responseDTO.setSastResult(scanEntity.getSastResult());
        responseDTO.setScaResult(scanEntity.getScaResult());

        return responseDTO;
    }
}
