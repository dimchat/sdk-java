/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
 * ==============================================================================
 */
package chat.dim.format;

public final class HexCoder implements DataCoder {

    @Override
    public String encode(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length << 1);
        for (byte ch : data) {
            sb.append(HEX_CHARS[(ch & 0xF0) >> 4]);
            sb.append(HEX_CHARS[ch & 0x0F]);
        }
        return sb.toString();
    }

    @Override
    public byte[] decode(String string) {
        int length = string.length();
        int odd = length % 2;
        byte[] buffer = new byte[length / 2 + odd];
        if (length == 0) {
            return buffer;
        }
        int index = 0, offset = 0;
        if (odd == 1) {
            // add first char
            buffer[0] = (byte) HEX_VALUES[string.charAt(0)];
            index = 1;
            offset = 1;
        }
        int hi, lo;
        for (; index < length; index += 2, offset += 1) {
            hi = HEX_VALUES[string.charAt(index)];
            lo = HEX_VALUES[string.charAt(index + 1)];
            if (hi < 0 || lo < 0) {
                throw new IndexOutOfBoundsException("hex string error: " + string);
            }
            buffer[offset] = (byte) ((hi << 4) + lo);
        }
        return buffer;
    }

    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
    };
    private static final int[] HEX_VALUES = new int[256];

    static {
        int index;
        for (index = 0; index < 256; ++index) {
            HEX_VALUES[index] = -1;
        }
        for (index = '0'; index <= '9'; ++index) {
            HEX_VALUES[index] = index - '0';
        }
        for (index = 'a'; index <= 'f'; ++index) {
            HEX_VALUES[index] = index - 'a' + 10;
        }
        for (index = 'A'; index <= 'F'; ++index) {
            HEX_VALUES[index] = index - 'A' + 10;
        }
    }
}
