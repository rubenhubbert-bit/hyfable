package com.hytale.fablescript.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Central logger for the FableScript plugin.
 *
 * <p>Wraps {@link java.util.logging.Logger} with:
 * <ul>
 *   <li>Rolling file handler that rotates after 10 MB</li>
 *   <li>Structured prefix: {@code [FableScript/INFO] message}</li>
 *   <li>Separate debug flag controlled by config</li>
 * </ul>
 * </p>
 */
public class FableLogger {

    private static final String LOGGER_NAME = "FableScript";
    private static final int MAX_LOG_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final int LOG_FILE_COUNT = 5;

    private final Logger logger;
    private boolean debugEnabled;

    /**
     * Initialises the logger, creating the logs/ directory and rotating file handler.
     *
     * @param dataFolder plugin data folder; logs are placed in {@code <dataFolder>/logs/}
     * @param debugEnabled whether DEBUG-level messages are emitted
     */
    public FableLogger(File dataFolder, boolean debugEnabled) {
        this.logger = Logger.getLogger(LOGGER_NAME);
        this.debugEnabled = debugEnabled;
        this.logger.setUseParentHandlers(false);
        setupConsoleHandler();
        setupFileHandler(dataFolder);
    }

    private void setupConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new FableFormatter());
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
        logger.setLevel(Level.ALL);
    }

    private void setupFileHandler(File dataFolder) {
        try {
            Path logDir = dataFolder.toPath().resolve("logs");
            Files.createDirectories(logDir);
            String pattern = logDir.resolve("fablescript-%g.log").toString();
            FileHandler fileHandler = new FileHandler(pattern, MAX_LOG_BYTES, LOG_FILE_COUNT, true);
            fileHandler.setFormatter(new FableFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            logger.warning("[FableScript] Could not create log file handler: " + e.getMessage());
        }
    }

    /** Logs an INFO-level message. */
    public void info(String message) {
        logger.info(message);
    }

    /** Logs a DEBUG-level message; suppressed when debug mode is off. */
    public void debug(String message) {
        if (debugEnabled) {
            logger.fine("[DEBUG] " + message);
        }
    }

    /** Logs a WARN-level message. */
    public void warn(String message) {
        logger.warning(message);
    }

    /** Logs a WARN-level message with exception stack trace. */
    public void warn(String message, Throwable t) {
        logger.log(Level.WARNING, message, t);
    }

    /** Logs a SEVERE-level message with exception. */
    public void severe(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }

    /**
     * Enables or disables debug output at runtime (used by /fable debug toggle).
     *
     * @param enabled true to enable verbose debug logging
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }

    /** @return true if debug logging is currently enabled */
    public boolean isDebugEnabled() { return debugEnabled; }

    /** Custom log record formatter for FableScript output. */
    private static class FableFormatter extends Formatter {
        private static final DateTimeFormatter TIME_FMT =
                DateTimeFormatter.ofPattern("HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            String level = record.getLevel() == Level.INFO ? "INFO"
                    : record.getLevel() == Level.WARNING ? "WARN"
                    : record.getLevel() == Level.SEVERE ? "ERROR"
                    : record.getLevel().getName();
            return String.format("[%s][FableScript/%s] %s%n",
                    LocalDateTime.now().format(TIME_FMT),
                    level,
                    formatMessage(record));
        }
    }
}
