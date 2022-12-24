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
package chat.dim.ecc;

public class Secp256k1 {

    /**
     *  Make key pair for SECP256k1
     *
     * @return 96 bytes; 0-63 for public key, 64-95 for private key
     */
    public static native byte[] makeKeys();

    /**
     *  Compute public key from private key for SECP256k1
     *
     * @param priKey - 32 bytes, private key
     * @return 64 bytes, public key
     */
    public static native byte[] computePublicKey(byte[] priKey);

    /**
     *  Verify message hash & signature for SECP256k1
     *
     * @param pubKey    - 64 bytes, public key,
     * @param msgHash   - 32 bytes, sha256(data)
     * @param signature - 64 bytes, signature from der
     * @return 0 for error, 1 for matched
     */
    public static native int verify(byte[] pubKey, byte[] msgHash, byte[] signature);

    /**
     *  Sign message hash for SECP256k1
     *
     * @param priKey  - 32 bytes, private key
     * @param msgHash - 32 bytes, sha256(data)
     * @return <=72 bytes, signature
     */
    public static native byte[] sign(byte[] priKey, byte[] msgHash);

    /*/
    static {
        try {
            System.loadLibrary("Secp256k1");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    /*/
}
