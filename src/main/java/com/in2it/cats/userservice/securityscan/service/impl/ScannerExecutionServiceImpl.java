package com.in2it.cats.userservice.securityscan.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.in2it.cats.userservice.securityscan.service.ScannerExecutionService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ScannerExecutionServiceImpl implements ScannerExecutionService {

    private static final String SEMGREP_PATH =
            "/home/jitendrakumar/.local/bin/semgrep";

    private static final String OSV_SCANNER_PATH =
            "/home/jitendrakumar/.local/bin/osv-scanner";

    private static final String CHECKOV_PATH =
            "/home/jitendrakumar/.local/bin/checkov";

    private static final long SCAN_TIMEOUT_MINUTES = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String runSast(Path sourcePath) {
        return executeScanner(
                SEMGREP_PATH,
                "scan",
                "--config",
                "auto",
                "--json",
                sourcePath.toAbsolutePath().toString()
        );
    }

    @Override
    public String runSca(Path sourcePath) {
        return executeScanner(
                OSV_SCANNER_PATH,
                "scan",
                "source",
                "-r",
                sourcePath.toAbsolutePath().toString(),
                "--format",
                "json"
        );
    }

    @Override
    public String runCheckov(Path sourcePath) {
        return executeScanner(
                CHECKOV_PATH,
                "-d",
                sourcePath.toAbsolutePath().toString(),
                "--output",
                "json"
        );
    }

    private boolean isValidJson(String output) {
        try {
            objectMapper.readTree(output);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public BothScanResult runBoth(Path sourcePath) {

        CompletableFuture<String> sastFuture =
                CompletableFuture.supplyAsync(() -> runSast(sourcePath));

        CompletableFuture<String> scaFuture =
                CompletableFuture.supplyAsync(() -> runSca(sourcePath));

        CompletableFuture.allOf(sastFuture, scaFuture).join();

        return new BothScanResult(
                sastFuture.join(),
                scaFuture.join()
        );
    }

    private String executeScanner(String... command) {

        Path outputFile = null;
        Process process = null;
        Path errorFile = null;

        try {
            outputFile = Files.createTempFile("scanner-output-", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            errorFile = Files.createTempFile("scanner-error-", ".log");
            processBuilder.redirectErrorStream(false);
            processBuilder.redirectOutput(outputFile.toFile());
            processBuilder.redirectError(errorFile.toFile());

            process = processBuilder.start();

            boolean completed = process.waitFor(
                    SCAN_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                process.waitFor();

                throw new IllegalStateException(
                        "Security scanner timed out");
            }

            int exitCode = process.exitValue();
            String output = Files.readString(outputFile);
            String errorOutput = Files.readString(errorFile);

            boolean isOsvScanner = command[0].equals(OSV_SCANNER_PATH);
            boolean isCheckovScanner = command[0].equals(CHECKOV_PATH);

            if (isOsvScanner && exitCode == 1) {
                return output;
            }
            if (isCheckovScanner && exitCode == 1 && isValidJson(output)) {
                return output;
            }

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Security scanner failed with exit code "
                                + exitCode
                                + ". Output: " + output
                                + ". Error: " + errorOutput);
            }

            return output;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to execute security scanner. Check its installation and permissions.",
                    exception);

        } catch (InterruptedException exception) {
            if (process != null) {
                process.destroyForcibly();
            }

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Security scan was interrupted", exception);

        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // Ignore temporary output cleanup failure.
                }
            }
            if (errorFile != null) {
                try {
                    Files.deleteIfExists(errorFile);
                } catch (IOException ignored) {
                    // Ignore temporary error file cleanup failure.
                }
            }
        }
    }

    public static class BothScanResult {

        private final String sastResult;
        private final String scaResult;

        public BothScanResult(String sastResult, String scaResult) {
            this.sastResult = sastResult;
            this.scaResult = scaResult;
        }

        public String getSastResult() {
            return sastResult;
        }

        public String getScaResult() {
            return scaResult;
        }
    }
}