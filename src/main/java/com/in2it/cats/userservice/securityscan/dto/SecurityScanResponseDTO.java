
package com.in2it.cats.userservice.securityscan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SecurityScanResponseDTO {

    private String scanId;
    private String connectionId;
    private String repositoryId;
    private String repositoryName;
    private String scanType;
    private String status;
    private String message;
    private String sastResult;
    private String scaResult;
    private LocalDateTime createdAt;
}
