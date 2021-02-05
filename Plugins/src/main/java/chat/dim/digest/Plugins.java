/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.digest;

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

public interface Plugins {

    /*
     *  Data Digesters
     */
    static void registerDataDigesters() {

        /*
         *  Digest
         */
        RIPEMD160.digester = new DataDigester() {
            @Override
            public byte[] digest(byte[] data) {
                RIPEMD160Digest digest = new RIPEMD160Digest();
                digest.update(data, 0, data.length);
                byte[] out = new byte[20];
                digest.doFinal(out, 0);
                return out;
            }
        };
        Keccak256.digester = new DataDigester() {
            @Override
            public byte[] digest(byte[] data) {
                KeccakDigest digest = new KeccakDigest(256);
                digest.update(data, 0, data.length);
                byte[] out = new byte[digest.getDigestSize()];
                digest.doFinal(out, 0);
                return out;
            }
        };
    }
}
