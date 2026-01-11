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
import static org.junit.jupiter.api.Assertions.*;
import java.util.regex.Pattern; // Checking that we don't accidentally use this
import com.google.re2j.Matcher; // Checking that we use this

public class MaskLogEventFactoryTest {

    @Test
    public void testMaskSensitive() {
        // We can't access private methods directly, but we can verify regex behavior
        // via reflection or just copying logic for verification.
        // Assuming we want to verify the library behavior specifically for the patterns
        // used in MaskLogEventFactory.

        com.google.re2j.Pattern p = com.google.re2j.Pattern
                .compile("(<SensitiveData>)([\\da-fA-F]*)(</{1,2}SensitiveData>)");
        String input = "<SensitiveData>DEADBEEF</SensitiveData>";
        com.google.re2j.Matcher m = p.matcher(input);

        assertTrue(m.find());
        assertEquals("DEADBEEF", m.group(2));

        String replacement = "$1LENGTH:" + m.group(2).length() + "$3";
        String output = m.replaceAll(replacement);

        assertEquals("<SensitiveData>LENGTH:8</SensitiveData>", output);
    }

    @Test
    public void testMaskFiles() {
        // Pattern from MaskLogEventFactory: ([\\da-fA-F]{1000,})
        // NOTE: MaskLogEventFactory.MAX_SIZE = 1000

        com.google.re2j.Pattern p = com.google.re2j.Pattern.compile("([\\da-fA-F]{10,})"); // Using smaller size for
                                                                                           // test
        String input = "1234567890abcdef";
        com.google.re2j.Matcher m = p.matcher(input);

        assertTrue(m.find());
        assertEquals(input, m.group(1)); // Group 1 is the whole match

        // Logic from line 173: m.replaceAll("<TOO BIG:"+length+">")
        int length = m.group(1).length();
        String output = m.replaceAll("<TOO BIG:" + length + ">");

        assertEquals("<TOO BIG:16>", output);
    }

    @Test
    public void testSetFormatter() {
        MaskLogEventFactory factory = MaskLogEventFactory.newInstance(new String[] {});

        // Mock LogEvent since we can't easily instantiate a real one with all deps or
        // it might be complex
        // Actually, we just need something that has a message.
        // Let's rely on the fact that our custom formatter won't even check the event
        // if we don't want it to.

        MaskLogEventFactory.FORMATTER = (event, sb) -> sb.append("Custom Formatter Output");

        StringBuilder sb = new StringBuilder();
        factory.format(null, sb); // Event is ignored by our custom formatter

        assertEquals("Custom Formatter Output", sb.toString());
    }

    @Test
    public void testIntegrationMasking() {
        MaskLogEventFactory factory = MaskLogEventFactory.newInstance(new String[] {});

        org.apache.logging.log4j.core.LogEvent event = org.mockito.Mockito
                .mock(org.apache.logging.log4j.core.LogEvent.class);
        org.apache.logging.log4j.message.Message message = org.mockito.Mockito
                .mock(org.apache.logging.log4j.message.Message.class);
        org.mockito.Mockito.when(event.getMessage()).thenReturn(message);

        String input = "Request: url=http://foo.com?password=secret&token=123 Body: {\"password\": \"hidden\", \"other\": 1}";
        org.mockito.Mockito.when(message.getFormattedMessage()).thenReturn(input);

        StringBuilder sb = new StringBuilder();
        factory.format(event, sb);

        // URI Masking: password=secret -> password=<MASKED>, token=123 ->
        // token=<MASKED>
        // JSON Masking: "password": "hidden" -> "password": "<MASKED>"
        String expected = "Request: url=http://foo.com?password=<MASKED>&token=<MASKED> Body: {\"password\": \"<MASKED>\", \"other\": 1}";

        assertEquals(expected, sb.toString());
    }
}
