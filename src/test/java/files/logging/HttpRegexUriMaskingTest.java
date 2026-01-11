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

public class HttpRegexUriMaskingTest {

    @Test
    public void testMaskUri() {
        HttpRegexUriMasking masking = new HttpRegexUriMasking(Arrays.asList("token", "secret"));

        String input = "http://example.com?token=12345&other=abc&secret=verysecret";
        String expected = "http://example.com?token=<MASKED>&other=abc&secret=<MASKED>";

        assertEquals(expected, masking.mask(input));
    }

    @Test
    public void testMaskUriAtEnd() {
        HttpRegexUriMasking masking = new HttpRegexUriMasking(Collections.singletonList("token"));

        String input = "http://example.com?token=12345";
        String expected = "http://example.com?token=<MASKED>";

        assertEquals(expected, masking.mask(input));
    }

    @Test
    public void testMaskUriNoMatch() {
        HttpRegexUriMasking masking = new HttpRegexUriMasking(Collections.singletonList("token"));

        String input = "http://example.com?other=12345";
        assertEquals(input, masking.mask(input));
    }

    @Test
    public void testMaskMultipleParams() {
        HttpRegexUriMasking masking = new HttpRegexUriMasking(Arrays.asList("p1", "p2"));
        String input = "url?p1=v1&p2=v2&p3=v3";
        String expected = "url?p1=<MASKED>&p2=<MASKED>&p3=v3";
        assertEquals(expected, masking.mask(input));
    }
}
