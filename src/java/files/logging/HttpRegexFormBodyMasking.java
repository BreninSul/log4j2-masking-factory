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

public class HttpRegexFormBodyMasking implements HttpBodyMasking {
    protected Collection<String> fields;
    protected String emptyBody = "";
    protected String maskedBody = "<MASKED>";
    protected Map<String, Collection<Pattern>> regexList;

    public HttpRegexFormBodyMasking(Collection<String> fields) {
        this.fields = fields;
        Map<String, Collection<Pattern>> map = new HashMap<>();
        for (String f : fields) {
            List<Pattern> patterns = new ArrayList<>();
            // Improved pattern to handle parameters in free text (stop at & or whitespace)
            // Groups: 1=key, 2=delimiter, 3=value
            patterns.add(Pattern.compile("(" + f + ")(=)([^&\\s]*)"));
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
                .filter(entry -> message.contains(entry.getKey() + "="))
                .flatMap(entry -> entry.getValue().stream())
                .flatMap(regex -> {
                    Matcher matcher = regex.matcher(maskedMessage);
                    List<LoggingRange> matches = new ArrayList<>();
                    while (matcher.find()) {
                        int groupStart = matcher.start(3);
                        int groupEnd = matcher.end(3) - 1;
                        if (groupEnd >= groupStart) {
                            matches.add(new LoggingRange(groupStart, groupEnd));
                        }
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
        return HttpBodyType.FORM;
    }

}
