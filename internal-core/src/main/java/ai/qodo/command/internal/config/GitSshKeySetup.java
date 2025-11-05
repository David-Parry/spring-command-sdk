/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.Set;

@Component
public class GitSshKeySetup {
    private static final Logger logger = LoggerFactory.getLogger(GitSshKeySetup.class);
    private static final String GIT_SSH_PRIVATE_KEY = "GIT_SSH_PRIVATE_KEY";
    private final String sshDir;
    private final String privateKeyPath;

    public GitSshKeySetup() {
        String userHome = System.getProperty("user.home");
        this.sshDir = userHome + "/.ssh";
        this.privateKeyPath = sshDir + "/aws_ecdsa";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void setupSshKeys() {
        try {
            String privateKeyEnv = System.getenv(GIT_SSH_PRIVATE_KEY);

            if (privateKeyEnv == null || privateKeyEnv.isEmpty()) {
                logger.error("No SSH private key found in environment variable {}", GIT_SSH_PRIVATE_KEY);
                return;
            }

            // Create .ssh directory if needed
            createSshDirectory();

            // Decode and write private key
            String privateKey = decodeKey(privateKeyEnv);
            writePrivateKey(privateKey);

            // Create SSH config
            createSshConfig();
            
            // Try to add key to SSH agent if available
            addKeyToAgent();

            logger.info("SSH keys successfully configured @ {} for Git operations", privateKeyPath);

        } catch (Exception e) {
            throw new RuntimeException("Failed to setup SSH keys", e);
        }
    }

    protected String encodeKey() {
        try {
            Path keyPath = Paths.get(privateKeyPath);
            if (!Files.exists(keyPath)) {
                logger.error("SSH private key file not found at {}", privateKeyPath);
                throw new RuntimeException("SSH private key file not found at " + privateKeyPath);
            }
            String contents = Files.readString(keyPath, StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read SSH private key from " + privateKeyPath, e);
        }
    }

    protected String decodeKey(String encodedKey) {
        try {
            // Try to decode as base64 first
            return new String(Base64.getDecoder().decode(encodedKey));
        } catch (IllegalArgumentException e) {
            // If not base64, assume it's already plain text
            return encodedKey;
        }
    }

    private void createSshDirectory() throws IOException {
        Path sshPath = Paths.get(sshDir);

        if (!Files.exists(sshPath)) {
            Files.createDirectories(sshPath);

            // Set directory permissions to 700 (rwx------)
            if (isUnix()) {
                Set<PosixFilePermission> perms = Set.of(PosixFilePermission.OWNER_READ,
                                                        PosixFilePermission.OWNER_WRITE,
                                                        PosixFilePermission.OWNER_EXECUTE);
                Files.setPosixFilePermissions(sshPath, perms);
                logger.info("Unix style OS set up ssh directory with {}", perms);
            }
        }
    }

    private void writePrivateKey(String privateKey) throws IOException {
        Path keyPath = Paths.get(privateKeyPath);

        // Write the private key
        Files.writeString(keyPath, privateKey);

        // Set strict permissions: 600 (rw-------)
        if (isUnix()) {
            Set<PosixFilePermission> perms = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(keyPath, perms);
            logger.info("Setup unix style OS set up ssh directory with {} and keypath {}", perms, keyPath);
        }

        logger.debug("Private key written to: {}", privateKeyPath);
    }

    private void createSshConfig() throws IOException {
        String sshConfigPath = sshDir + "/config";
        Path configPath = Paths.get(sshConfigPath);

        String sshConfig = """
                Host github.com
                    HostName github.com
                    User git
                    IdentityFile %s
                    IdentitiesOnly yes
                    StrictHostKeyChecking accept-new
                
                """.formatted(privateKeyPath);

        Files.writeString(configPath, sshConfig);

        // Set permissions: 600
        if (isUnix()) {
            Set<PosixFilePermission> perms = Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(configPath, perms);
        }

        logger.info("SSH config written to: {} ", sshConfigPath);
    }

    private void addKeyToAgent() {
        try {
            // Check if SSH agent is running
            String sshAuthSock = System.getenv("SSH_AUTH_SOCK");
            if (sshAuthSock != null && !sshAuthSock.isEmpty()) {
                logger.info("SSH_AUTH_SOCK detected: {}, attempting to add key to agent", sshAuthSock);
                
                // Try to add the key to the SSH agent
                ProcessBuilder pb = new ProcessBuilder("ssh-add", privateKeyPath);
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    logger.info("Successfully added SSH key to agent");
                } else {
                    logger.warn("Failed to add SSH key to agent, exit code: {}", exitCode);
                }
            } else {
                logger.info("SSH agent not detected (SSH_AUTH_SOCK not set), skipping agent configuration");
            }
        } catch (Exception e) {
            logger.warn("Error adding key to SSH agent: {}", e.getMessage());
        }
    }
    
    private boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }
}