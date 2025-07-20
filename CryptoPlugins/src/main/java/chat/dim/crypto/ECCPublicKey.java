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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Map;

import chat.dim.ecc.ECCKeys;
import chat.dim.utils.CryptoUtils;

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

    private final ECPublicKey publicKey;

    public ECCPublicKey(Map<String, Object> dictionary) throws NoSuchFieldException {
        super(dictionary);
        publicKey = getKey();
    }

    private ECPublicKey getKey() throws NoSuchFieldException {
        String data = getString("data");
        if (data == null) {
            throw new NoSuchFieldException("ECC public key data not found");
        }
        return (ECPublicKey) ECCKeys.decodePublicKey(data);
    }

    @Override
    public byte[] getData() {
        if (publicKey == null) {
            return null;
        }
        return ECCKeys.getPointData(publicKey);
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        try {
            Signature signer = CryptoUtils.getSignature(CryptoUtils.ECDSA_SHA256);
            signer.initVerify(publicKey);
            signer.update(data);
            return signer.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return false;
        }
    }
}
