
package com.in2it.cats.userservice.securityscan.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "security_scans")
public class SecurityScanEntity {

    @Id
    private String id;

    private String connectionId;
    private String repositoryId;
    private String repositoryName;
    private String scanType;
    private String status;
    private String message;
    private String sastResult;
    private String scaResult;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
