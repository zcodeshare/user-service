
package com.in2it.cats.userservice.securityscan.service.impl;

import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsRepositoryResponseDTO;
import com.in2it.cats.userservice.azuredevops.entity.AzureDevOpsConnectionEntity;
import com.in2it.cats.userservice.azuredevops.repository.AzureDevOpsConnectionRepository;
import com.in2it.cats.userservice.azuredevops.service.AzureDevOpsService;
import com.in2it.cats.userservice.azuredevops.service.impl.AzureDevOpsEncryptionService;
import com.in2it.cats.userservice.securityscan.service.RepositorySourceService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class RepositorySourceServiceImpl implements RepositorySourceService {

    private static final String BASE_URL = "https://dev.azure.com/";
    private static final String SCAN_BRANCH = "dev";

    private final AzureDevOpsService azureDevOpsService;
    private final AzureDevOpsConnectionRepository connectionRepository;
    private final AzureDevOpsEncryptionService encryptionService;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public RepositorySourceServiceImpl(AzureDevOpsService azureDevOpsService, AzureDevOpsConnectionRepository connectionRepository,
            AzureDevOpsEncryptionService encryptionService) {

        this.azureDevOpsService = azureDevOpsService;
        this.connectionRepository = connectionRepository;
        this.encryptionService = encryptionService;
    }

    @Override
    public Path downloadRepository(String connectionId, String repositoryId) {

        Path temporaryDirectory = null;

        try {
            // Step 1: Find the saved Azure DevOps connection.
            AzureDevOpsConnectionEntity connection = connectionRepository.findById(connectionId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Azure DevOps connection not found")
                            );

            // Step 2: Verify that the repository belongs to this connection.
            List<AzureDevOpsRepositoryResponseDTO> repositories = azureDevOpsService.getRepositoriesByConnectionId(connectionId);

            AzureDevOpsRepositoryResponseDTO selectedRepository =
                    repositories.stream()
                            .filter(repository ->
                                    repositoryId.equals(repository.getRepositoryId())
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Repository not found for this connection")
                            );

            // Step 3: Decrypt the saved PAT.
            String pat = encryptionService.decrypt(connection.getEncryptedPersonalAccessToken()
            );

            // Step 4: Prepare the Azure DevOps ZIP download URL.
            String organization = connection.getOrganizationName();
            String project = selectedRepository.getProjectName();

            String encodedOrganization = encodePathSegment(organization);
            String encodedProject = encodePathSegment(project);
            String encodedBranch = encodeQueryValue(SCAN_BRANCH);

            String archiveUrl = BASE_URL
                    + encodedOrganization + "/"
                    + encodedProject
                    + "/_apis/git/repositories/"
                    + encodePathSegment(repositoryId)
                    + "/items?scopePath=%2F"
                    + "&recursionLevel=Full"
                    + "&download=true"
                    + "&$format=zip"
                    + "&versionDescriptor.version="
                    + encodedBranch
                    + "&versionDescriptor.versionType=branch"
                    + "&api-version=7.1";

            // Step 5: Authenticate using Basic Auth.
            String credentials = Base64.getEncoder().encodeToString((":" + pat).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(archiveUrl))
                    .header("Authorization", "Basic " + credentials)
                    .header("Accept", "application/zip")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            // Step 6: Check the download response.
            if (response.statusCode() != 200) {
                try (InputStream body = response.body()) {
                    body.transferTo(java.io.OutputStream.nullOutputStream());
                }

                throw new IllegalStateException(
                        "Repository download failed. HTTP status: " + response.statusCode());
            }

            // Step 7: Save the downloaded ZIP in a temporary directory.
            temporaryDirectory = Files.createTempDirectory("security-scan-");

            Path zipFile = temporaryDirectory.resolve("repository.zip");

            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, zipFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Step 8: Verify that the response is a ZIP archive.
            if (!isValidZip(zipFile)) {
                throw new IOException("Azure DevOps response is not a valid ZIP archive");
            }

            // Step 9: Extract the repository safely.
            Path sourceDirectory = Files.createDirectory(temporaryDirectory.resolve("source"));

            int extractedFiles = extractZip(zipFile, sourceDirectory);

            if (extractedFiles == 0) {
                throw new IOException("The downloaded archive contains no files");
            }

            Files.deleteIfExists(zipFile);
            return sourceDirectory;

        } catch (Exception exception) {

            if (temporaryDirectory != null) {
                deleteDirectory(temporaryDirectory);
            }

            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new IllegalStateException("Unable to download repository source", exception);
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private boolean isValidZip(Path zipFile) throws IOException {

        byte[] signature = new byte[4];

        try (InputStream inputStream = Files.newInputStream(zipFile)) {
            int bytesRead = inputStream.read(signature);

            return bytesRead >= 4
                    && signature[0] == 'P'
                    && signature[1] == 'K'
                    && ((signature[2] == 3 && signature[3] == 4)
                            || (signature[2] == 5 && signature[3] == 6)
                            || (signature[2] == 7 && signature[3] == 8)
            );
        }
    }

    private int extractZip(Path zipFile, Path destination) throws IOException {

        Path normalizedDestination = destination.toAbsolutePath().normalize();

        int extractedFiles = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {

            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {

                Path target = normalizedDestination.resolve(entry.getName()).normalize();

                // Prevent ZIP path traversal.
                if (!target.startsWith(normalizedDestination)) {
                    throw new IOException("Invalid path found in repository archive");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());

                    Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                    extractedFiles++;
                }

                zipInputStream.closeEntry();
            }
        }

        return extractedFiles;
    }

    private void deleteDirectory(Path directory) {

        try (var paths = Files.walk(directory)) {
            paths.sorted((first, second) -> second.compareTo(first))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // A cleanup failure must not hide the original error.
                        }
                    });
        } catch (IOException ignored) {
            // A cleanup failure must not hide the original error.
        }
    }

    @Override
    public void cleanupRepository(Path sourcePath) {

        if (sourcePath == null) {
            return;
        }

        Path temporaryDirectory = sourcePath.getParent();

        if (temporaryDirectory != null) {
            deleteDirectory(temporaryDirectory);
        }
    }
}
