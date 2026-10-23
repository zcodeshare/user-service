
package com.in2it.cats.userservice.securityscan.service;

import java.nio.file.Path;

public interface RepositorySourceService {

    Path downloadRepository(String connectionId, String repositoryId);
    void cleanupRepository(Path sourcePath);
}
