package files.logging;

import com.google.re2j.Pattern;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

public class JsonMaskingPerformanceTest {

    @Test
    public void comparePerformance() {
        List<String> fields = Arrays.asList("password", "credit_card", "token", "secret", "cvv");
        HttpRegexJsonBodyMasking re2jMasking = new HttpRegexJsonBodyMasking(fields);
        JavaRegexJsonBodyMasking javaMasking = new JavaRegexJsonBodyMasking(fields);

        String json = "{\n" +
                "  \"user\": \"breninsul\",\n" +
                "  \"password\": \"superSecret123!\",\n" +
                "  \"credit_card\": \"1234-5678-9012-3456\",\n" +
                "  \"nested\": {\n" +
                "    \"token\": \"abcdef123456\",\n" +
                "    \"public\": \"data\"\n" +
                "  },\n" +
                "  \"array\": [\n" +
                "    {\"secret\": \"hidden\", \"cvv\": 123}\n" +
                "  ],\n" +
                "  \"description\": \"Some long description text to make the body larger and more realistic.\"\n" +
                "}";

        // Validate correctness first
        String re2jResult = re2jMasking.mask(json);
        String javaResult = javaMasking.mask(json);
        if (!re2jResult.equals(javaResult)) {
            System.err.println("re2j result: " + re2jResult);
            System.err.println("java result: " + javaResult);
            throw new RuntimeException("Implementations do not produce the same result!");
        }

        int warmup = 10000;
        int iterations = 50000;

        // Warmup RE2/J
        for (int i = 0; i < warmup; i++) {
            re2jMasking.mask(json);
        }
        // Measure RE2/J
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            re2jMasking.mask(json);
        }
        long re2jTime = System.nanoTime() - start;

        // Warmup Java
        for (int i = 0; i < warmup; i++) {
            javaMasking.mask(json);
        }
        // Measure Java
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            javaMasking.mask(json);
        }
        long javaTime = System.nanoTime() - start;

        System.out.println("RE2/J time: " + (re2jTime / 1_000_000) + " ms");
        System.out.println("Java  time: " + (javaTime / 1_000_000) + " ms");
        System.out.printf("Factor: %.2f x (Java is better)\n", (double) re2jTime / javaTime);
    }

    // --- Java Regex Implementation (Copy of HttpRegexJsonBodyMasking
    // functionality) ---

    public static class JavaRegexJsonBodyMasking implements HttpBodyMasking {
        protected Collection<String> fields;
        protected String emptyBody = "";
        protected String maskedBody = "<MASKED>";
        // Using java.util.regex.Pattern
        protected Map<String, Collection<java.util.regex.Pattern>> regexList;

        public JavaRegexJsonBodyMasking(Collection<String> fields) {
            this.fields = fields;
            Map<String, Collection<java.util.regex.Pattern>> map = new HashMap<>();
            for (String f : fields) {
                List<java.util.regex.Pattern> patterns = new ArrayList<>();
                // int, float or bool
                patterns.add(java.util.regex.Pattern.compile(
                        "\"(" + f + ")\"\\s*:\\s*([+-]?\\d*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|true|false)\\s*(,|\\})"));
                // string
                patterns.add(java.util.regex.Pattern.compile("\"(" + f + ")\"\\s*:\\s*\"((\\\\.|[^\"\\\\])*)\""));
                // array
                patterns.add(
                        java.util.regex.Pattern.compile(
                                "\"(" + f + ")\"\\s*:\\s*\\[(\\s*(?:\"(?:\\\\.|[^\"\\\\])*\"\\s*,?\\s*)*)\\]"));
                // object
                patterns.add(java.util.regex.Pattern
                        .compile("\"(" + f + ")\"\\s*:\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}"));

                map.put(f, patterns);
            }
            this.regexList = map;
        }

        @Override
        public String mask(String message) {
            if (message == null) {
                return emptyBody;
            }
            StringBuilder maskedMessage = new StringBuilder(message);

            List<LoggingRange> ranges = regexList.entrySet().stream()
                    .filter(entry -> message.contains("\"" + entry.getKey() + "\""))
                    .flatMap(entry -> entry.getValue().stream())
                    .flatMap(regex -> {
                        java.util.regex.Matcher matcher = regex.matcher(maskedMessage);
                        List<LoggingRange> matches = new ArrayList<>();
                        while (matcher.find()) {
                            int groupStart = matcher.start(2);
                            int groupEnd = matcher.end(2) - 1;
                            matches.add(new LoggingRange(groupStart, groupEnd));
                        }
                        return matches.stream();
                    })
                    .sorted((r1, r2) -> Integer.compare(r2.last, r1.last))
                    .collect(Collectors.toList());

            List<LoggingRange> finalRanges = new ArrayList<>();
            for (LoggingRange range : ranges) {
                boolean isContained = false;
                for (LoggingRange r : ranges) {
                    if (r != range && r.first <= range.first && r.last >= range.last) {
                        isContained = true;
                        break;
                    }
                }
                if (!isContained) {
                    finalRanges.add(range);
                }
            }

            for (LoggingRange range : finalRanges) {
                maskedMessage.replace(range.first, range.last + 1, maskedBody);
            }

            return maskedMessage.toString();
        }

        @Override
        public HttpBodyType type() {
            return HttpBodyType.JSON;
        }
    }
}
