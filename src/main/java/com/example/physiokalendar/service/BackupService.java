package com.example.physiokalendar.service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Value("${backup.dir:/backup}")
    private String backupDir;

    @Value("${spring.datasource.url:jdbc:mysql://physio-test-db:3306/physiocalendar_test}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:physiouser}")
    private String dbUser;

    @Value("${spring.datasource.password:testpassword}")
    private String dbPassword;

    @Value("${backup.script.path:/usr/local/bin/mysql_backup.sh}")
    private String backupScriptPath;

    @Value("${backup.docker.container:}")
    private String backupDockerContainer;

    /**
     * Creates a backup using the Docker backup container's script
     * Falls back to direct mysqldump if the script is not available
     *
     * @param backupType Type of backup: "full", "incremental", or null (defaults to auto)
     * @return Backup file path or null if failed
     */
    public String createBackup(String backupType) throws IOException {
        try {
            if (backupType == null) {
                backupType = "auto";
            }
            logger.info("Starting backup using Docker backup container script... (type: {})", backupType);

            // Try to use the backup script first (for Docker environments)
            String result = createBackupUsingScript(backupType);
            if (result != null) {
                logger.info("Backup created successfully: {}", result);
                return result;
            }
        } catch (IOException e) {
            logger.warn("Backup script method failed, trying alternative methods: {}", e.getMessage());
        }

        // Fallback 1: Use docker exec to run backup in container (for local dev without mysqldump)
        if (backupDockerContainer != null && !backupDockerContainer.isEmpty()) {
            try {
                logger.info("Attempting backup via docker exec in container: {}", backupDockerContainer);
                String result = createBackupUsingDockerExec(backupType);
                if (result != null) {
                    logger.info("Backup created successfully via docker exec: {}", result);
                    return result;
                }
            } catch (IOException e) {
                logger.warn("docker exec method failed: {}", e.getMessage());
            }
        }

        // Fallback 2: Use mysqldump directly (requires mysqldump installed locally)
        try {
            logger.info("Attempting direct mysqldump backup...");
            String result = createBackupUsingMySQLDump();
            if (result != null) {
                logger.info("Backup created successfully using mysqldump: {}", result);
                return result;
            }
        } catch (IOException e) {
            logger.warn("mysqldump method failed: {}", e.getMessage());
        }

        throw new IOException("Failed to create backup using any available method");
    }

    /**
     * Overloaded method for backwards compatibility
     * Defaults to auto backup type
     */
    public String createBackup() throws IOException {
        return createBackup(null);
    }

    /**
     * Uses the existing backup container script to create a backup
     *
     * @param backupType Type of backup: "full", "incremental", or "auto"
     */
    private String createBackupUsingScript(String backupType) throws IOException {
        try {
            String dbName = extractDatabaseName(datasourceUrl);
            String host = extractHost(datasourceUrl);
            String port = extractPort(datasourceUrl);

            // Build environment variables for the script
            Map<String, String> env = new HashMap<>();
            env.put("DB_HOST", host);
            env.put("DB_PORT", port);
            env.put("DB_NAME", dbName);
            env.put("DB_USER", dbUser);
            env.put("DB_PASSWORD", dbPassword);
            env.put("BACKUP_DIR", backupDir);

            logger.info("Executing backup script: {} (type: {}) with DB_HOST={}, DB_PORT={}, DB_NAME={}",
                backupScriptPath, backupType, host, port, dbName);

            // Create backup script process with backup type argument
            ProcessBuilder pb = new ProcessBuilder("bash", backupScriptPath, backupType);
            pb.environment().putAll(env);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Capture output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Backup script: {}", line);
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Backup script completed successfully");

                // Find the most recently created backup file
                return findLatestBackup();
            } else {
                logger.error("Backup script failed with exit code: {}", exitCode);
                throw new IOException("Backup script failed with exit code: " + exitCode + "\nOutput:\n" + output);
            }
        } catch (InterruptedException e) {
            logger.error("Backup script process was interrupted", e);
            Thread.currentThread().interrupt();
            throw new IOException("Backup process was interrupted", e);
        } catch (Exception e) {
            logger.error("Error executing backup script", e);
            throw new IOException("Error executing backup script: " + e.getMessage(), e);
        }
    }

    /**
     * Creates backup using mysqldump command directly
     * Used as fallback when the backup script is not available
     */
    private String createBackupUsingMySQLDump() throws IOException {
        try {
            String dbName = extractDatabaseName(datasourceUrl);
            String host = extractHost(datasourceUrl);
            String port = extractPort(datasourceUrl);

            logger.info("Starting mysqldump: database={}, host={}, port={}", dbName, host, port);

            // Create backup directory if not exists
            File backupDirectory = new File(backupDir);
            if (!backupDirectory.exists()) {
                if (!backupDirectory.mkdirs()) {
                    throw new IOException("Failed to create backup directory: " + backupDir);
                }
                logger.info("Created backup directory: {}", backupDir);
            }

            // Detect environment from database name
            String env = "dev";
            if (dbName.contains("prod")) {
                env = "prod";
            } else if (dbName.contains("test")) {
                env = "test";
            }

            // Generate timestamp matching script format: {env}_full_YYYYMMDD_HHMMSS.sql.gz
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_full_%s.sql.gz", env, timestamp);
            String backupFilePath = new File(backupDir, filename).getAbsolutePath();

            // Convert Windows path to Unix style for bash command
            String bashFilePath = backupFilePath.replace("\\", "/");

            logger.info("Output file: {}", backupFilePath);

            // Build mysqldump command via shell pipe
            String passwordPart = (dbPassword != null && !dbPassword.isEmpty()) ? "-p" + dbPassword : "";
            String command = String.format(
                "mysqldump --single-transaction --routines --triggers --events -h%s -P%s -u%s %s %s | gzip -c > \"%s\"",
                host, port, dbUser, passwordPart, dbName, bashFilePath
            );

            logger.info("Executing: mysqldump ... | gzip > {}", backupFilePath);

            // Use bash to handle the pipe
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(false);

            Process process;
            try {
                process = pb.start();
            } catch (IOException e) {
                logger.error("Failed to start backup process", e);
                throw new IOException("mysqldump or gzip command not found. Ensure MySQL tools are installed.", e);
            }

            // Capture error output
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            StringBuilder errors = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                logger.warn("mysqldump: {}", errorLine);
                errors.append(errorLine).append("\n");
            }

            // Wait for completion
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMsg = "mysqldump | gzip failed with exit code: " + exitCode;
                if (errors.length() > 0) {
                    errorMsg += "\n" + errors;
                }
                logger.error(errorMsg);
                new File(backupFilePath).delete();
                throw new IOException(errorMsg);
            }

            logger.info("Backup created successfully: {}", backupFilePath);
            return backupFilePath;
        } catch (InterruptedException e) {
            logger.error("Backup process was interrupted", e);
            Thread.currentThread().interrupt();
            throw new IOException("Backup process was interrupted", e);
        }
    }

    /**
     * Creates backup using docker exec to run the backup script in a container
     * Used for local development when mysqldump is not installed
     */
    private String createBackupUsingDockerExec(String backupType) throws IOException {
        try {
            String dbName = extractDatabaseName(datasourceUrl);

            // Detect environment from database name
            String env = "dev";
            if (dbName.contains("prod")) {
                env = "prod";
            } else if (dbName.contains("test")) {
                env = "test";
            }

            logger.info("Running backup via docker exec in container: {}", backupDockerContainer);

            // Build the docker exec command to run the backup script
            String command = String.format(
                "docker exec %s bash /scripts/mysql_backup.sh %s",
                backupDockerContainer, backupType != null ? backupType : "full"
            );

            logger.info("Executing: {}", command);

            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            // Prevent Git Bash on Windows from converting Unix paths like /scripts/ to Windows paths
            pb.environment().put("MSYS_NO_PATHCONV", "1");
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Capture stdout for the backup filename
            BufferedReader outputReader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String outputLine;
            while ((outputLine = outputReader.readLine()) != null) {
                logger.info("docker exec: {}", outputLine);
                output.append(outputLine).append("\n");
            }

            // Capture stderr
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            StringBuilder errors = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                logger.warn("docker exec stderr: {}", errorLine);
                errors.append(errorLine).append("\n");
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMsg = "docker exec backup failed with exit code: " + exitCode;
                if (errors.length() > 0) {
                    errorMsg += "\n" + errors;
                }
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }

            // Find the latest backup file created
            String latestBackup = findLatestBackup();
            if (latestBackup != null) {
                return latestBackup;
            }

            // Fallback: construct expected filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_full_%s.sql.gz", env, timestamp);
            return new File(backupDir, filename).getAbsolutePath();

        } catch (InterruptedException e) {
            logger.error("Docker exec process was interrupted", e);
            Thread.currentThread().interrupt();
            throw new IOException("Docker exec process was interrupted", e);
        }
    }

    /**
     * Finds the most recently created backup file
     */
    private String findLatestBackup() {
        try {
            File backupDirectory = new File(backupDir);
            if (!backupDirectory.exists()) {
                return null;
            }

            // Match both old format and new format ({env}_full_... / {env}_inc_...)
            File[] files = backupDirectory.listFiles((dir, name) ->
                name.matches("(backup_|(dev|test|prod)_(full|inc)_).*\\.sql(\\.gz)?"));

            if (files != null && files.length > 0) {
                // Sort by modification time, descending
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                // Check if the latest file was just created (within last 2 minutes)
                long timeDiff = System.currentTimeMillis() - files[0].lastModified();
                if (timeDiff < 120000) { // 2 minutes
                    return files[0].getAbsolutePath();
                }
            }
        } catch (Exception e) {
            logger.error("Error finding latest backup", e);
        }
        return null;
    }

    /**
     * Lists all backup files in the backup directory
     * @return List of backup file information
     */
    public List<Map<String, Object>> getBackupFiles() {
        List<Map<String, Object>> backups = new ArrayList<>();
        try {
            File backupDirectory = new File(backupDir);
            if (!backupDirectory.exists()) {
                return backups;
            }

            // Match both old format (backup_...) and new format ({env}_full_... / {env}_inc_...)
            File[] files = backupDirectory.listFiles((dir, name) ->
                name.matches("(backup_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}|(dev|test|prod)_(full|inc)_\\d{8}_\\d{6}(_\\d{3})?)\\.sql\\.gz"));

            if (files != null) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                for (File file : files) {
                    Map<String, Object> backup = new HashMap<>();
                    backup.put("filename", file.getName());
                    backup.put("size", formatFileSize(file.length()));
                    backup.put("sizeBytes", file.length());
                    backup.put("created", new java.util.Date(file.lastModified()));
                    backups.add(backup);
                }
            }
        } catch (Exception e) {
            logger.error("Error listing backups", e);
        }
        return backups;
    }

    /**
     * Deletes a backup file
     * @param filename The name of the backup file
     * @return true if deletion was successful
     */
    public boolean deleteBackup(String filename) {
        try {
            // Security: prevent directory traversal attacks
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                logger.warn("Attempted directory traversal in backup deletion: {}", filename);
                return false;
            }

            File backupFile = new File(backupDir, filename);
            if (backupFile.exists() && backupFile.delete()) {
                logger.info("Backup deleted: {}", filename);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error deleting backup", e);
            return false;
        }
    }

    private String extractDatabaseName(String datasourceUrl) {
        // Extract database name from URL like: jdbc:mysql://host:port/dbname
        try {
            String[] parts = datasourceUrl.split("/");
            String dbPart = parts[parts.length - 1];
            // Remove query parameters if any
            return dbPart.split("\\?")[0];
        } catch (Exception e) {
            logger.error("Could not extract database name from URL", e);
            return "physiocalendar";
        }
    }

    private String extractHost(String datasourceUrl) {
        // Extract host from URL like: jdbc:mysql://host:port/dbname
        try {
            String[] parts = datasourceUrl.split("//");
            if (parts.length < 2) return "physio-test-db";
            String hostPart = parts[1].split("/")[0].split(":")[0];
            return hostPart;
        } catch (Exception e) {
            logger.error("Could not extract host from URL", e);
            return "physio-test-db";
        }
    }

    private String extractPort(String datasourceUrl) {
        // Extract port from URL like: jdbc:mysql://host:port/dbname
        try {
            String[] parts = datasourceUrl.split("://");
            if (parts.length < 2) return "3306";
            String hostPart = parts[1].split("/")[0];
            if (hostPart.contains(":")) {
                return hostPart.split(":")[1];
            }
            return "3306";
        } catch (Exception e) {
            logger.error("Could not extract port from URL", e);
            return "3306";
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
