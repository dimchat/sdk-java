/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import java.util.Map;

import chat.dim.type.Dictionary;

public class Base64Data extends Dictionary implements TransportableData {

    private byte[] data;

    public Base64Data(Map<String, Object> dictionary) {
        super(dictionary);
        // lazy load
        data = null;
    }

    public Base64Data(String algorithm, byte[] binary) {
        super();
        // algorithm: base64
        assert algorithm.equals(TransportableData.BASE_64) : "unsupported algorithm: " + algorithm;
        put("algorithm", algorithm);
        // binary data (lazy encode)
        data = binary;
    }

    @Override
    public String getAlgorithm() {
        return getString("algorithm", null);
    }

    @Override
    public byte[] getData() {
        byte[] binary = data;
        if (binary == null) {
            String base64 = getString("data", null);
            if (base64 != null) {
                data = binary = Base64.decode(base64);
            }
        }
        return binary;
    }

    private String encodeData() {
        String base64 = getString("data", null);
        if (base64 == null) {
            // field 'data' not exists, check binary data
            byte[] binary = data;
            if (binary != null) {
                // encode data string
                base64 = Base64.encode(binary);
                put("data", base64);
            }
        }
        assert base64 != null : "TED data should not be empty";
        return base64;
    }

    @Override
    public String toString() {
        String base64 = encodeData();
        if (base64 != null) {
            return base64;
        }
        // TODO: other field?
        return JSONMap.encode(toMap());
    }

    @Override
    public Object toObject() {
        return toString();
    }
}
