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
package chat.dim;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import chat.dim.crypto.AsymmetricKey;
import chat.dim.crypto.ECCPrivateKey;
import chat.dim.crypto.ECCPublicKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.RSAPrivateKey;
import chat.dim.crypto.RSAPublicKey;
import chat.dim.digest.DataDigester;
import chat.dim.digest.Keccak256;
import chat.dim.digest.RIPEMD160;
import chat.dim.format.JSON;
import chat.dim.format.ObjectCoder;

public class NativePluginLoader {

    private boolean isLoaded = false;

    /**
     *  Register core factories
     */
    public boolean load() {
        if (isLoaded) {
            // already loaded
            return false;
        } else {
            isLoaded = true;
            resetSecurityProvider();
        }

        registerDataCoders();
        registerDataDigesters();

        registerAsymmetricKeyFactories();

        // OK
        return true;
    }

    private void resetSecurityProvider() {

        Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider != null) {
            System.out.println(BouncyCastleProvider.PROVIDER_NAME + " old version: " + provider.getVersion());
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        }
        provider = new BouncyCastleProvider();
        System.out.println(BouncyCastleProvider.PROVIDER_NAME + " new version: " + provider.getVersion());
        Security.addProvider(provider);

    }

    /**
     *  Data coders
     */
    private void registerDataCoders() {

        JSON.coder = new ObjectCoder<Object>() {

            @Override
            public String encode(Object container) {
                /*/
                String s = com.alibaba.fastjson.JSON.toJSONString(container);
                return s.getBytes(Charset.forName("UTF-8"));
                */
                return com.alibaba.fastjson.JSON.toJSONString(container);
            }

            @Override
            public Object decode(String json) {
                return com.alibaba.fastjson.JSON.parse(json);
            }
        };

    }

    /**
     *  Data digesters
     */
    private void registerDataDigesters() {

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

    /**
     *  Asymmetric key parsers
     */
    private void registerAsymmetricKeyFactories() {

        PrivateKey.Factory rsaPri = new PrivateKey.Factory() {

            @Override
            public PrivateKey generatePrivateKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", AsymmetricKey.RSA);
                return parsePrivateKey(key);
            }

            @Override
            public PrivateKey parsePrivateKey(Map<String, Object> key) {
                try {
                    return new RSAPrivateKey(key);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        PrivateKey.setFactory(AsymmetricKey.RSA, rsaPri);
        PrivateKey.setFactory("SHA256withRSA", rsaPri);
        PrivateKey.setFactory("RSA/ECB/PKCS1Padding", rsaPri);

        PrivateKey.setFactory(AsymmetricKey.ECC, new PrivateKey.Factory() {

            @Override
            public PrivateKey generatePrivateKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", AsymmetricKey.ECC);
                return parsePrivateKey(key);
            }

            @Override
            public PrivateKey parsePrivateKey(Map<String, Object> key) {
                try {
                    return new ECCPrivateKey(key);
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        PublicKey.Factory rsaPub = new PublicKey.Factory() {

            @Override
            public PublicKey parsePublicKey(Map<String, Object> key) {
                try {
                    return new RSAPublicKey(key);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        PublicKey.setFactory(AsymmetricKey.RSA, rsaPub);
        PublicKey.setFactory("SHA256withRSA", rsaPub);
        PublicKey.setFactory("RSA/ECB/PKCS1Padding", rsaPub);

        PublicKey.setFactory(AsymmetricKey.ECC, new PublicKey.Factory() {

            @Override
            public PublicKey parsePublicKey(Map<String, Object> key) {
                try {
                    return new ECCPublicKey(key);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

    }

}
