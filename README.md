# Log4j2 Masking Factory

A Log4j2 extension that provides robust masking for sensitive information in log messages. This library replaces the default message pattern converter to automatically sanitize logs, protecting PII, credentials, and preventing large binary dumps from flooding your logs.

## Features

### 1. Automatic Sensitive Data Redaction
- **Explicit Tags**: Content wrapped in `<SensitiveData>...</SensitiveData>` tags is automatically masked.
- **Pattern Matching**: Masks matches for pre-configured sensitive patterns.

### 2. Large File/Binary Protection
- Automatically detects large Hex or Base64 strings (likely binary file dumps).
- Truncates them to prevent log bloat, replacing the content with a placeholder like `<TOO BIG:size>`.
- Default threshold: 1000 characters.

### 3. HTTP Request/Response Masking
Integrated support for masking sensitive data in HTTP-like messages:
- **URI Parameters**: automatically masks values for sensitive query parameters (e.g., `?password=...`, `?token=...`).
- **JSON Bodies**: Parses JSON structures in the log message and masks values for sensitive keys.
- **Form Data**: Masks fields in URL-encoded form bodies.

**Default Sensitive Keys:**
`password`, `token`, `access_token`, `client_secret`, `authorization`, `api_key`, `secret`.

## Technology
- Utilizes **RE2/J** for regular expressions to ensure linear time matching complexity $O(n)$, preventing "Regular Expression Denial of Service" (ReDoS) attacks and catastrophic backtracking common with standard Java Regex on complex patterns.

## Usage

### 1. Add Dependency
Include the library in your build:

**Gradle (Kotlin)**
```kotlin
implementation("io.github.breninsul:log4j2-masking-factory:1.0.1")
```

### 2. Configure Log4j2
Enable the plugin by adding the package `files.logging` to the `packages` attribute of your `Configuration` element in `log4j2.xml`.

The plugin registers itself for the identifiers: `%m`, `%msg`, `%message`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" packages="files.logging">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <!-- usage of %msg or %m will now invoke MaskLogEventFactory -->
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

## Customization
The masking logic is implemented in `MaskLogEventFactory`.
- **Masking Toggle**: Can be enabled/disabled via `MaskLogEventFactory.IS_ENABLED_MASKING`.
- **File Size Limit**: Configurable via `MaskLogEventFactory.MAX_LOGGING_FILE_SIZE`.
- **Formatter**: The internal formatter strategy can be customized programmatically via `MaskLogEventFactory.FORMATTER` if deeper customization is needed.

### Customizing Masking Keys
You can customize the keys that are masked for URI, JSON, and Form data by replacing the static fields directly (e.g., during application startup):

```java
import files.logging.MaskLogEventFactory;
import files.logging.HttpRegexUriMasking;
import files.logging.HttpRegexJsonBodyMasking;
import files.logging.HttpRegexFormBodyMasking;
import java.util.Set;

// ...

// Change URI parameter masking keys
MaskLogEventFactory.URI_FIELDS= Set.of("token", "custom_param");
    //or
MaskLogEventFactory.uriMasking = new HttpRegexUriMasking(Set.of("token", "custom_param"));

// Change JSON field masking keys
MaskLogEventFactory.JSON_FIELDS= Set.of("password","cvv");
    //or
MaskLogEventFactory.jsonMasking = new HttpRegexJsonBodyMasking(Set.of("password", "cvv"));

// Change Form field masking keys
MaskLogEventFactory.FORM_FIELDS= Set.of("secret", "ssn");
    //or
MaskLogEventFactory.formMasking = new HttpRegexFormBodyMasking(Set.of("secret", "ssn"));
```
These changes take effect immediately for subsequent log events.

## Default Configuration

The library includes a default `log4j2.xml` file with pre-configured masking.

To use your own configuration, create a `log4j2.xml` file in your application's `src/main/resources` directory. Log4j2 will automatically detect and use your configuration file instead of the default one provided by the library.

You can also specify the configuration file location using the system property:
```bash
-Dlog4j.configurationFile=path/to/your/log4j2.xml
```

### Log File Location
You can customize the log file directory and filename using environment variables without replacing the configuration file:

| Environment Variable | Default   | Description |
|---------------------|-----------|-------------|
| `LOG_FOLDER`        | `logs`    | Directory path for log files |
| `LOG_FILENAME`      | `app.log` | Name of the valid log file |

**Example:**
```bash
export LOG_FOLDER=/var/logs/my-service
export LOG_FILENAME=service-output.log
java -jar build/libs/myapp.jar
```

## License
This project is licensed under the MIT License - see the LICENSE file for details.
