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

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import java.util.*;
import java.util.stream.Collectors;

public class HttpRegexJsonBodyMasking implements HttpBodyMasking {
    protected Collection<String> fields;
    protected String emptyBody = "";
    protected String maskedBody = "<MASKED>";
    protected Map<String, Collection<Pattern>> regexList;

    public HttpRegexJsonBodyMasking(Collection<String> fields) {
        this.fields = fields;
        Map<String, Collection<Pattern>> map = new HashMap<>();
        for (String f : fields) {
            List<Pattern> patterns = new ArrayList<>();
            // int or bool: "($f)"\s*:\s*([+-]?\d+|true|false)\s*(,|\})
            patterns.add(Pattern.compile("\"(" + f + ")\"\\s*:\\s*([+-]?\\d+|true|false)\\s*(,|\\})"));
            // string: "($f)"\s*:\s*"((\\"|[^"])*)"
            patterns.add(Pattern.compile("\"(" + f + ")\"\\s*:\\s*\"((\\\\\"|[^\"])*)\""));
            // array: "($f)"\s*:\s*\[(\s*(?:"(?:\\.|[^"\\])*"\s*,?\s*)*)\]
            patterns.add(
                    Pattern.compile("\"(" + f + ")\"\\s*:\\s*\\[(\\s*(?:\"(?:\\\\.|[^\"\\\\])*\"\\s*,?\\s*)*)\\]"));
            // object: "($f)"\s*:\s*\{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}
            patterns.add(Pattern.compile("\"(" + f + ")\"\\s*:\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}"));

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
                    Matcher matcher = regex.matcher(maskedMessage);
                    List<LoggingRange> matches = new ArrayList<>();
                    while (matcher.find()) {
                        // Kotlin: matcher.start(2), end(2)-1
                        int groupStart = matcher.start(2);
                        int groupEnd = matcher.end(2) - 1;
                        matches.add(new LoggingRange(groupStart, groupEnd));
                    }
                    return matches.stream();
                })
                .sorted((r1, r2) -> Integer.compare(r2.last, r1.last)) // Sort by last desc
                .collect(Collectors.toList());

        // Filter ranges contained in others
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

        // Apply replacements
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
