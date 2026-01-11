/*
 * MIT License
 *
 * Copyright (c) 2026 BreninSul
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package files.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

import java.util.Set;
import java.util.function.BiConsumer;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a custom LogEventPatternConverter that masks sensitive
 * information and file paths in
 */
@Plugin(name = "CustomMessagePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "m", "msg", "message" })
@PerformanceSensitive("allocation")
public class MaskLogEventFactory extends LogEventPatternConverter {

    public static Integer MAX_LOGGING_FILE_SIZE = 1000;
    public static boolean IS_ENABLED_MASKING = true;

    public static Set<Pattern> FILE_PATTERNS = Stream.of(
            Pattern.compile("([\\da-fA-F]{" + MAX_LOGGING_FILE_SIZE + ",})"), // HEX
            Pattern.compile("([\\da-zA-Z+\\/]{" + MAX_LOGGING_FILE_SIZE + ",}={0,3})")// BASE64
    ).collect(Collectors.toSet());

    public static Set<Pattern> SENSITIVE_PATTERNS = Stream.of(
            Pattern.compile("(<SensitiveData>)([\\da-fA-F]*)(</{1,2}SensitiveData>)"), // HEX
            Pattern.compile("(?i)(3c53656e736974697665446174613e)([\\da-fA-F]*)(?i)(3c2f53656e736974697665446174613e)"), // HEX
            // //
            // pattern
            Pattern.compile("(<SensitiveData>)([\\da-zA-Z+\\/]*={0,3})(</{1,2}SensitiveData>)")// BASE64
    ).collect(Collectors.toSet());

    public static Set<String> URI_FIELDS = Stream.of(
            "password", "token", "access_token", "client_secret", "authorization", "api_key", "secret")
            .collect(Collectors.toSet());

    public static Set<String> JSON_FIELDS = Stream.of(
            "password", "token", "access_token", "client_secret", "authorization", "api_key", "secret")
            .collect(Collectors.toSet());

    public static Set<String> FORM_FIELDS = Stream.of(
            "password", "token", "access_token", "client_secret", "authorization", "api_key", "secret")
            .collect(Collectors.toSet());

    public static HttpUriMasking uriMasking = new HttpRegexUriMasking(URI_FIELDS);
    public static HttpBodyMasking jsonMasking = new HttpRegexJsonBodyMasking(JSON_FIELDS);
    public static HttpBodyMasking formMasking = new HttpRegexFormBodyMasking(FORM_FIELDS);

    MaskLogEventFactory(final String[] options) {
        super("m", "m");
    }

    public static BiConsumer<LogEvent, StringBuilder> FORMATTER = (a, b) -> {
        if (IS_ENABLED_MASKING) {
            formatAndMaskLog(a, b);
        } else {
            formatNoMasking(a, b);
        }
    };

    public static void formatNoMasking(LogEvent event, StringBuilder outputMessage) {
        outputMessage.append(event.getMessage().getFormattedMessage());
    }

    public static void formatAndMaskLog(LogEvent event, StringBuilder outputMessage) {
        try {
            String original = event.getMessage().getFormattedMessage();
            String maskedSensitive = maskSensitive(original);
            String maskedFiles = maskFiles(maskedSensitive);

            // Apply new maskings
            String maskedUri = uriMasking.mask(maskedFiles);
            String maskedJson = jsonMasking.mask(maskedUri);
            String maskedForm = formMasking.mask(maskedJson);

            outputMessage.append(maskedForm);
        } catch (Exception e) {
            outputMessage.append("EXCEPTION IN LOGGER!").append(e.getClass().getSimpleName()).append(":")
                    .append(e.getMessage());
        }
    }

    public static MaskLogEventFactory newInstance(final String[] options) {
        return new MaskLogEventFactory(options);
    }

    @Override
    public void format(LogEvent event, StringBuilder outputMessage) {
        FORMATTER.accept(event, outputMessage);
    }

    public static String maskSensitive(String message) {
        try {
            String reduced = SENSITIVE_PATTERNS.stream()
                    .reduce(message, (msg, p) -> {
                        Matcher m = p.matcher(msg);
                        boolean found = m.find();
                        if (found) {
                            String secondGroup = m.group(2);
                            int secondGroupLength = secondGroup.length();
                            String replaced = m.replaceAll(
                                    String.format(
                                            "%s%s%s",
                                            "$1",
                                            "LENGTH:" + secondGroupLength,
                                            "$3"));
                            return replaced;
                        } else {
                            return msg;
                        }
                    },
                            (one, second) -> one);
            return reduced;
        } catch (Throwable t) {
            return "EXCEPTION IN LOGGER!!!!!!!!!! " + message;
        }
    }

    public static String maskFiles(String message) {
        try {
            return FILE_PATTERNS.stream()
                    .reduce(message, (msg, p) -> {
                        Matcher m = p.matcher(msg);
                        return m.find()
                                ? replaceAll(m)
                                : msg;
                    }, (one, second) -> one);
        } catch (Throwable t) {
            return "EXCEPTION IN LOGGER!" + message;
        }
    }

    public static String replaceAll(Matcher m) {
        Integer length = m.group(1).length();
        return m.replaceAll("<TOO BIG:" + length + ">");
    }

}
