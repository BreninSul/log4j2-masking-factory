package files.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import java.io.File;

/**
 * Helper class to verify Log4j2 configuration in a separate process
 * where we can control environment variables cleanly.
 */
public class Log4j2EnvConfigVerifier {
    public static void main(String[] args) {
        try {
            // Locate the log4j2.xml file
            // We assume the CWD is the project root
            File file = new File("src/resource/log4j2.xml");
            if (!file.exists()) {
                System.err.println("Could not find configuration file at: " + file.getAbsolutePath());
                System.exit(1);
            }

            // reconfigure context with this file
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(file.toURI());
            context.reconfigure();

            Configuration config = context.getConfiguration();
            RollingFileAppender appender = (RollingFileAppender) config.getAppender("RollingFile");

            if (appender == null) {
                System.out.println("Appender 'RollingFile' not found");
                System.exit(2);
            }

            System.out.println("FileName: " + appender.getFileName());
            System.out.println("FilePattern: " + appender.getFilePattern());

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
    }
}
