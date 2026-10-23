
package com.in2it.cats.userservice.securityscan.service.impl;

import com.in2it.cats.userservice.securityscan.service.ScannerExecutionService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class ScannerExecutionServiceImpl implements ScannerExecutionService {

    private static final String SEMGREP_PATH = "/home/jitendrakumar/.local/bin/semgrep";

    private static final String OSV_SCANNER_PATH = "/home/jitendrakumar/.local/bin/osv-scanner";

    private static final long SCAN_TIMEOUT_MINUTES = 10;

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

    private String executeScanner(String... command) {

        Path outputFile = null;
        Process process = null;

        try {
            outputFile = Files.createTempFile("scanner-output-", ".json");

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(outputFile.toFile());

            process = processBuilder.start();

            boolean completed = process.waitFor(SCAN_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (!completed) {
                process.destroyForcibly();
                process.waitFor();

                throw new IllegalStateException("Security scanner timed out");
            }

            int exitCode = process.exitValue();
            String output = Files.readString(outputFile);

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Security scanner failed with exit code "
                                + exitCode + ". Output: " + output
                );
            }

            return output;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to execute security scanner. Check its installation and permissions.", exception);
        } catch (InterruptedException exception) {
            if (process != null) {
                process.destroyForcibly();
            }

            Thread.currentThread().interrupt();

            throw new IllegalStateException("Security scan was interrupted", exception);
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    // Temporary output cleanup failure.
                }
            }
        }
    }
}
