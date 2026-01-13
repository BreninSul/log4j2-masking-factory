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

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpRegexJsonBodyMaskingTest {

    @Test
    public void testMaskJsonString() {
        HttpRegexJsonBodyMasking masking = new HttpRegexJsonBodyMasking(Collections.singletonList("password"));

        String input = "{\"user\": \"bren\", \"password\": \"secret123\", \"other\": \"value\"}";
        // The regex replaces "password": "..." with "password": <MASKED>
        // Note: The regex preserves the key and surrounding chars but replaces value.
        // Let's check regex logic in HttpRegexJsonBodyMasking.
        // Pattern: "\"(" + f + ")\"\\s*:\\s*\"((\\\\\"|[^\"])*)\""
        // Group 2 is the value inside quotes (including quotes? No, the regex for
        // string is different).
        // Wait, checking code:
        // patterns.add(Pattern.compile("\"(" + f +
        // ")\"\\s*:\\s*\"((\\\\\"|[^\"])*)\""));
        // Group 2 is ((\\\\\"|[^\"])*) which is the content inside quotes.
        // But the matcher.start(2) is used.
        // So it replaces only the content inside quotes?
        // Wait, the Kotlin code:
        // """"($f)"\s*:\s*"((\\"|[^"])*)""""
        // matcher.start(2)
        // replacement replaces range with <MASKED>
        // So: "password": "secret" -> "password": "<MASKED>" (if quotes are outside
        // group 2)
        // My regex: "\"(" + f + ")\"\\s*:\\s*\"((\\\\\"|[^\"])*)\""
        // Group 1: key
        // Group 2: value content (inside quotes)
        // The quotes around value are OUTSIDE group 2.
        // See: ... : \s* " ( ... ) "
        // So removing group 2 preserves quotes.
        // Result: "password": "<MASKED>"

        String expected = "{\"user\": \"bren\", \"password\": \"<MASKED>\", \"other\": \"value\"}";

        assertEquals(expected, masking.mask(input));
    }

    @Test
    public void testMaskJsonNumber() {
        HttpRegexJsonBodyMasking masking = new HttpRegexJsonBodyMasking(Collections.singletonList("id"));

        String input = "{\"id\": 12345, \"name\": \"foo\"}";
        // Regex: "\"(" + f + ")\"\\s*:\\s*([+-]?\\d+|true|false)\\s*(,|\\})"
        // Group 2 is the number.
        // Result: "id": <MASKED>, "name"...

        String expected = "{\"id\": <MASKED>, \"name\": \"foo\"}";
        assertEquals(expected, masking.mask(input));
    }

    @Test
    public void testMaskJsonArray() {
        HttpRegexJsonBodyMasking masking = new HttpRegexJsonBodyMasking(Collections.singletonList("secrets"));

        String input = "{\"secrets\": [\"a\",\"b\"], \"other\": 1}";
        // Regex for array: ... : \s* \[ ... \]
        // Group 2 is content inside []
        // Result: "secrets": [<MASKED>]

        String expected = "{\"secrets\": [<MASKED>], \"other\": 1}";
        assertEquals(expected, masking.mask(input));
    }

    @Test
    public void testMaskJsonObject() {
        HttpRegexJsonBodyMasking masking = new HttpRegexJsonBodyMasking(Collections.singletonList("details"));
        String input = "{\"details\": {\"a\":1}, \"other\": 2}";
        // Regex for object: ... : \s* \{ ... \}
        // Group 2 is content inside {}
        // Result: "details": {<MASKED>}

        String expected = "{\"details\": {<MASKED>}, \"other\": 2}";
        assertEquals(expected, masking.mask(input));
    }

    @Test
    public void testMaskJsonFloat() {
        HttpRegexJsonBodyMasking masking = new HttpRegexJsonBodyMasking(Collections.singletonList("amount"));

        String input = "{\"amount\": 12.34, \"other\": \"val\", \"amount2\": -0.005, \"sci\": 1.2e3}";
        // It should mask amount and amount2 if they were in the list.
        // Wait, I only added "amount" to the list.
        // Let's add all of them for this test, or just test one.
        // Let's test standard float.

        String expected = "{\"amount\": <MASKED>, \"other\": \"val\", \"amount2\": -0.005, \"sci\": 1.2e3}";
        assertEquals(expected, masking.mask(input));

        // Test with multiple fields
        masking = new HttpRegexJsonBodyMasking(Arrays.asList("amount", "amount2", "sci"));
        expected = "{\"amount\": <MASKED>, \"other\": \"val\", \"amount2\": <MASKED>, \"sci\": <MASKED>}";
        assertEquals(expected, masking.mask(input));
    }
}
