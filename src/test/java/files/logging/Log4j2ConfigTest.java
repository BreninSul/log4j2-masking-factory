package files.logging;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class Log4j2ConfigTest {

    @Test
    public void testEnvVariablesConfig() throws Exception {
        // Test with custom environment variables
        String output = runVerifier("custom_logs", "custom_app.log");

        // We expect "custom_logs/custom_app.log" in the output
        // Adjust for OS separator if needed, but Log4j usually normalizes or passes
        // through.
        assertTrue(output.contains("custom_logs/custom_app.log"),
                "Output should contain configured path. Actual output:\n" + output);

        assertTrue(output.contains("custom_logs/custom_app.log."),
                "Output should contain configured pattern. Actual output:\n" + output);
    }

    @Test
    public void testDefaultConfiguration() throws Exception {
        // Test with NO environment variables (defaults)
        String output = runVerifier(null, null);

        assertTrue(output.contains("logs/app.log"),
                "Output should contain default path 'logs/app.log'. Actual output:\n" + output);
    }

    private String runVerifier(String folder, String filename) throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = Log4j2EnvConfigVerifier.class.getName();

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(className);

        ProcessBuilder builder = new ProcessBuilder(command);

        // Setup Environment
        if (folder != null) {
            builder.environment().put("LOG_FOLDER", folder);
        } else {
            builder.environment().remove("LOG_FOLDER");
        }

        if (filename != null) {
            builder.environment().put("LOG_FILENAME", filename);
        } else {
            builder.environment().remove("LOG_FILENAME");
        }

        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            fail("Verifier process exited with code " + exitCode + ". Output:\n" + output);
        }

        return output.toString();
    }
}
