
package com.in2it.cats.userservice.securityscan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecurityScanRequestDTO {

    @NotBlank(message = "Connection ID is required")
    private String connectionId;

    @NotBlank(message = "Repository ID is required")
    private String repositoryId;

    @NotBlank(message = "Scan type is required")
    private String scanType;
}
