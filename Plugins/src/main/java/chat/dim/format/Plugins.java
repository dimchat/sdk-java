/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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

import java.security.Security;

public abstract class Plugins {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // Base58 coding
        Base58.coder = new DataCoder() {
            @Override
            public String encode(byte[] data) {
                return chat.dim.bitcoinj.Base58.encode(data);
            }

            @Override
            public byte[] decode(String string) {
                return chat.dim.bitcoinj.Base58.decode(string);
            }
        };

        // HEX coding
        Hex.coder = new DataCoder() {
            @Override
            public String encode(byte[] data) {
                return org.bouncycastle.util.encoders.Hex.toHexString(data);
            }

            @Override
            public byte[] decode(String string) {
                return org.bouncycastle.util.encoders.Hex.decode(string);
            }
        };

        // JsON format
        JSON.parser = new DataParser<Object>() {
            @Override
            public byte[] encode(Object container) {
                /*
                String s = com.alibaba.fastjson.JSON.toJSONString(container);
                return s.getBytes(Charset.forName("UTF-8"));
                */
                return com.alibaba.fastjson.JSON.toJSONBytes(container);
            }

            @Override
            public Object decode(byte[] json) {
                return com.alibaba.fastjson.JSON.parse(json);
            }
        };
    }
}
