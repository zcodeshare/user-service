
package com.in2it.cats.userservice.securityscan.service;

import java.nio.file.Path;

public interface ScannerExecutionService {

    String runSast(Path sourcePath);
    String runSca(Path sourcePath);
}
