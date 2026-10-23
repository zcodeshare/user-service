
package com.in2it.cats.userservice.azuredevops.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsConnectionRequestDTO;
import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsConnectionResponseDTO;
import com.in2it.cats.userservice.azuredevops.dto.AzureDevOpsRepositoryResponseDTO;
import com.in2it.cats.userservice.azuredevops.entity.AzureDevOpsConnectionEntity;
import com.in2it.cats.userservice.azuredevops.repository.AzureDevOpsConnectionRepository;
import com.in2it.cats.userservice.azuredevops.service.AzureDevOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AzureDevOpsServiceImpl implements AzureDevOpsService {

    private static final String API_VERSION = "7.1";
    private static final String BASE_URL = "https://dev.azure.com/";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final AzureDevOpsConnectionRepository connectionRepository;
    private final AzureDevOpsEncryptionService encryptionService;

    @Override
    public String testConnection(AzureDevOpsConnectionRequestDTO requestDTO) {

        String organization = encode(requestDTO.getOrganizationName());

        String url = BASE_URL + organization + "/_apis/projects?$top=1&api-version=" + API_VERSION;
        callAzureDevOps(url, requestDTO.getPersonalAccessToken());
        return "Azure DevOps connection successful";
    }

    @Override
    public String saveConnection(AzureDevOpsConnectionRequestDTO requestDTO) {

        if (connectionRepository.existsByConnectionName(requestDTO.getConnectionName())) {
            throw new IllegalArgumentException("Connection name already exists");
        }

        testConnection(requestDTO);

        String encryptedPAT = encryptionService.encrypt(requestDTO.getPersonalAccessToken());

        AzureDevOpsConnectionEntity entity = new AzureDevOpsConnectionEntity();

        entity.setConnectionName(requestDTO.getConnectionName());
        entity.setOrganizationName(requestDTO.getOrganizationName());
        entity.setProjectName(requestDTO.getProjectName());
        entity.setEncryptedPersonalAccessToken(encryptedPAT);
        entity.setStatus("CONNECTED");
        entity.setCreatedAt(LocalDateTime.now());

        connectionRepository.save(entity);

        return "Azure DevOps connection saved successfully";
    }

    @Override
    public List<AzureDevOpsRepositoryResponseDTO> getRepositories(AzureDevOpsConnectionRequestDTO requestDTO) {

        List<AzureDevOpsRepositoryResponseDTO> repositories = new ArrayList<>();

        String organization = encode(requestDTO.getOrganizationName());
        String projectName = requestDTO.getProjectName();
        String pat = requestDTO.getPersonalAccessToken();

        if (projectName != null && !projectName.isBlank()) {
            fetchRepositories(organization, projectName, pat, repositories);
        } else {
            fetchAllProjectRepositories(organization, pat, repositories);
        }

        return repositories;
    }

    private void fetchAllProjectRepositories(String organization, String pat, List<AzureDevOpsRepositoryResponseDTO> repositories) {

        String continuationToken = null;

        do {
            String url = BASE_URL + organization + "/_apis/projects?$top=100&api-version=" + API_VERSION;

            if (continuationToken != null && !continuationToken.isBlank()) {
                url += "&continuationToken=" + encode(continuationToken);
            }

            HttpResponse<String> response = callAzureDevOps(url, pat);
            JsonNode projects = readJson(response.body()).path("value");

            for (JsonNode project : projects) {
                String projectName = project.path("name").asText();

                fetchRepositories(organization, projectName, pat, repositories);
            }

            continuationToken = response.headers()
                    .firstValue("x-ms-continuationtoken")
                    .orElse(null);

        } while (continuationToken != null
                && !continuationToken.isBlank());
    }

    private void fetchRepositories(String organization, String projectName, String pat,
            List<AzureDevOpsRepositoryResponseDTO> repositories) {

        String url = BASE_URL + organization + "/" + encode(projectName) + "/_apis/git/repositories?api-version=" + API_VERSION;

        JsonNode repositoryList = readJson(callAzureDevOps(url, pat).body()).path("value");

        for (JsonNode repository : repositoryList) {

            AzureDevOpsRepositoryResponseDTO responseDTO = new AzureDevOpsRepositoryResponseDTO();
            responseDTO.setRepositoryId(repository.path("id").asText());
            responseDTO.setRepositoryName(repository.path("name").asText());
            responseDTO.setProjectName(repository.path("project").path("name").asText());
            responseDTO.setRemoteUrl(repository.path("remoteUrl").asText());
            responseDTO.setDefaultBranch(repository.path("defaultBranch").asText(""));
            repositories.add(responseDTO);
        }
    }

    private HttpResponse<String> callAzureDevOps(String url, String pat) {

        String credentials = ":" + pat;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(
                        credentials.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + encodedCredentials)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();

            if (statusCode == 401) {
                throw new IllegalArgumentException(
                        "Invalid or expired Azure DevOps PAT");
            }

            if (statusCode == 403) {
                throw new IllegalArgumentException("PAT does not have sufficient permissions");
            }

            if (statusCode == 404) {
                throw new IllegalArgumentException("Azure DevOps organization or project was not found " + "or is not accessible");
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalStateException("Azure DevOps API request failed with HTTP " + statusCode);
            }

            return response;

        } catch (IOException exception) {
            throw new IllegalStateException("Unable to communicate with Azure DevOps", exception);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Azure DevOps request was interrupted", exception);
        }
    }

    private JsonNode readJson(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read Azure DevOps response", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }


    @Override
    public List<AzureDevOpsConnectionResponseDTO> getConnections() {

        List<AzureDevOpsConnectionEntity> connections = connectionRepository.findAll();
        List<AzureDevOpsConnectionResponseDTO> responseList = new ArrayList<>();

        for (AzureDevOpsConnectionEntity connection : connections) {

            AzureDevOpsConnectionResponseDTO response = new AzureDevOpsConnectionResponseDTO();

            response.setId(connection.getId());
            response.setConnectionName(connection.getConnectionName());
            response.setOrganizationName(connection.getOrganizationName());
            response.setProjectName(connection.getProjectName());
            response.setStatus(connection.getStatus());
            response.setCreatedAt(connection.getCreatedAt());

            responseList.add(response);
        }

        return responseList;
    }


    @Override
    public List<AzureDevOpsRepositoryResponseDTO> getRepositoriesByConnectionId(
            String connectionId) {

        AzureDevOpsConnectionEntity connection = connectionRepository.findById(connectionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Azure DevOps connection not found"));


        String personalAccessToken = encryptionService.decrypt(connection.getEncryptedPersonalAccessToken());

        AzureDevOpsConnectionRequestDTO requestDTO = new AzureDevOpsConnectionRequestDTO();

        requestDTO.setConnectionName(connection.getConnectionName());
        requestDTO.setOrganizationName(connection.getOrganizationName());
        requestDTO.setProjectName(connection.getProjectName());
        requestDTO.setPersonalAccessToken(personalAccessToken);

        List<AzureDevOpsRepositoryResponseDTO> repositories = getRepositories(requestDTO);

        return repositories;
    }


}
