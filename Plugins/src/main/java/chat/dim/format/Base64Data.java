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

/**
 *  TED - Transportable Encoded Data
 */
public final class Base64Data extends Dictionary implements TransportableData {

    private final BaseDataWrapper wrapper;

    public Base64Data(Map<String, Object> dictionary) {
        super(dictionary);
        wrapper = new BaseDataWrapper(toMap());
    }

    public Base64Data(byte[] binary) {
        super();
        wrapper = new BaseDataWrapper(toMap());
        // encode algorithm
        wrapper.setAlgorithm(EncodeAlgorithms.BASE_64);
        // binary data
        if (binary != null) {
            wrapper.setData(binary);
        }
    }

    /**
     *  Encode Algorithm
     */

    @Override
    public String getAlgorithm() {
        return wrapper.getAlgorithm();
    }

    /**
     *  Binary Data
     */

    @Override
    public byte[] getData() {
        return wrapper.getData();
    }

    /**
     *  Encoding
     */

    @Override
    public Object toObject() {
        return toString();
    }

    @Override
    public String toString() {
        // 0. "{BASE64_ENCODE}"
        // 1. "base64,{BASE64_ENCODE}"
        return wrapper.toString();
    }

    /**
     *  Encode with 'Content-Type'
     */
    public String encode(String mimeType) {
        // 2. "data:image/png;base64,{BASE64_ENCODE}"
        return wrapper.encode(mimeType);
    }
}
