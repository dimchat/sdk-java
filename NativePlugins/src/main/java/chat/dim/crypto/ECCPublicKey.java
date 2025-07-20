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
package chat.dim.crypto;

import java.util.Map;

import chat.dim.digest.SHA256;
import chat.dim.ecc.Secp256k1;
import chat.dim.format.Hex;
import chat.dim.format.PEM;

/**
 *  ECC Public Key
 *
 *  <blockquote><pre>
 *  keyInfo format: {
 *      algorithm    : "ECC",
 *      curve        : "secp256k1",
 *      data         : "..." // base64_encode()
 *  }
 *  </pre></blockquote>
 */
public final class ECCPublicKey extends BasePublicKey {

    public ECCPublicKey(Map<String, Object> dictionary) throws NoSuchFieldException {
        super(dictionary);
    }

    @Override
    public byte[] getData() {
        String pem = getString("data");
        if (pem == null || pem.length() == 0) {
            throw new AssertionError("ECC public key data not found");
        }
        if (pem.length() == 66 || pem.length() == 130) {
            // Hex encode
            return Hex.decode(pem);
        } else {
            // PEM
            byte[] data = PEM.decodePublicKeyData(pem, "ECC");
            assert data != null && data.length > 32 : "ECC public key data error: " + pem;
            if (data.length > 65) {
                // FIXME: X.509 -> Uncompressed Point
                assert data.length == 88 : "unexpected ECC public key: " + pem;
                if (data[88-65] == 0x04) {
                    byte[] buffer = new byte[65];
                    System.arraycopy(data, 88-65, buffer, 0, 65);
                    data = buffer;
                } else {
                    throw new AssertionError("ECCKeyError: " + pem);
                }
            }
            return data;
        }
    }

    private byte[] getPubKey() {
        byte[] data = getData();
        if (data.length == 65) {
            byte[] buffer = new byte[64];
            System.arraycopy(data, 1, buffer, 0, 64);
            data = buffer;
        }
        return data;
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        byte[] hash = SHA256.digest(data);
        return Secp256k1.verify(getPubKey(), hash, signature) != 0;
    }
}
