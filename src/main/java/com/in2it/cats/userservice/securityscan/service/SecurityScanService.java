
package com.in2it.cats.userservice.securityscan.service;

import com.in2it.cats.userservice.securityscan.dto.SecurityScanRequestDTO;
import com.in2it.cats.userservice.securityscan.dto.SecurityScanResponseDTO;

public interface SecurityScanService {

    SecurityScanResponseDTO startScan(SecurityScanRequestDTO requestDTO);
}
