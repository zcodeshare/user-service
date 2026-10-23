
package com.in2it.cats.userservice.securityscan.controller;

import com.in2it.cats.userservice.securityscan.dto.SecurityScanRequestDTO;
import com.in2it.cats.userservice.securityscan.dto.SecurityScanResponseDTO;
import com.in2it.cats.userservice.securityscan.service.SecurityScanService;
import com.in2it.cats.userservice.securityscan.service.RepositorySourceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/security-scans")
public class SecurityScanController {

    private final SecurityScanService securityScanService;
    private final RepositorySourceService repositorySourceService;

    public SecurityScanController(SecurityScanService securityScanService, RepositorySourceService repositorySourceService) {
        this.securityScanService = securityScanService;
        this.repositorySourceService = repositorySourceService;
    }

    @PostMapping
    public SecurityScanResponseDTO startScan(@Valid @RequestBody SecurityScanRequestDTO requestDTO) {
        return securityScanService.startScan(requestDTO);
    }

    @GetMapping("/test-download/{connectionId}/{repositoryId}")
    public String testRepositoryDownload(@PathVariable String connectionId, @PathVariable String repositoryId) {

        Path sourcePath = repositorySourceService.downloadRepository(connectionId, repositoryId);
        return "Repository downloaded successfully to: " + sourcePath;
    }
}
