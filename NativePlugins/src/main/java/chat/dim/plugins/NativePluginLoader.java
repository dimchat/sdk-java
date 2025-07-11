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
package chat.dim.plugins;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import chat.dim.crypto.AsymmetricAlgorithms;
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
import chat.dim.utils.CryptoUtils;

public class NativePluginLoader implements Runnable {

    private boolean loaded = false;

    @Override
    public void run() {
        if (loaded) {
            // no need to load it again
            return;
        } else {
            // mark it to loaded
            loaded = true;
        }
        // try to load all plugins
        load();
    }

    /**
     *  Register core factories
     */
    protected void load() {
        prepare();

        registerDataCoders();
        registerDataDigesters();

        registerAsymmetricKeyFactories();

    }

    protected void prepare() {

        resetSecurityProvider();

    }
    protected void resetSecurityProvider() {

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
    protected void registerDataCoders() {

        registerJSONCoder();

    }
    protected void registerJSONCoder() {

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
    protected void registerDataDigesters() {

        registerRIPEMD160Digester();

        registerKeccak256Digester();

    }
    protected void registerRIPEMD160Digester() {
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
    }
    protected void registerKeccak256Digester() {
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
    protected void registerAsymmetricKeyFactories() {

        registerRSAKeyFactories();

        registerECCKeyFactories();

    }
    protected void registerRSAKeyFactories() {

        PrivateKey.Factory rsaPri = new PrivateKey.Factory() {

            @Override
            public PrivateKey generatePrivateKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", AsymmetricAlgorithms.RSA);
                return parsePrivateKey(key);
            }

            @Override
            public PrivateKey parsePrivateKey(Map<String, Object> key) {
                // check 'data'
                if (key.get("data") == null) {
                    // key.data should not be empty
                    assert false : "RSA key error: " + key;
                    return null;
                }
                try {
                    return new RSAPrivateKey(key);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        PrivateKey.setFactory(AsymmetricAlgorithms.RSA, rsaPri);
        PrivateKey.setFactory(CryptoUtils.RSA_SHA256, rsaPri);
        PrivateKey.setFactory(CryptoUtils.RSA_ECB_PKCS1, rsaPri);

        PublicKey.Factory rsaPub = new PublicKey.Factory() {

            @Override
            public PublicKey parsePublicKey(Map<String, Object> key) {
                // check 'data'
                if (key.get("data") == null) {
                    // key.data should not be empty
                    assert false : "RSA key error: " + key;
                    return null;
                }
                try {
                    return new RSAPublicKey(key);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        PublicKey.setFactory(AsymmetricAlgorithms.RSA, rsaPub);
        PublicKey.setFactory(CryptoUtils.RSA_SHA256, rsaPub);
        PublicKey.setFactory(CryptoUtils.RSA_ECB_PKCS1, rsaPub);

    }
    protected void registerECCKeyFactories() {

        PrivateKey.Factory eccPri = new PrivateKey.Factory() {

            @Override
            public PrivateKey generatePrivateKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", AsymmetricAlgorithms.ECC);
                return parsePrivateKey(key);
            }

            @Override
            public PrivateKey parsePrivateKey(Map<String, Object> key) {
                // check 'data'
                if (key.get("data") == null) {
                    // key.data should not be empty
                    assert false : "ECC key error: " + key;
                    return null;
                }
                try {
                    return new ECCPrivateKey(key);
                } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        PrivateKey.setFactory(AsymmetricAlgorithms.ECC, eccPri);
        PrivateKey.setFactory(CryptoUtils.ECDSA_SHA256, eccPri);

        PublicKey.Factory eccPub = new PublicKey.Factory() {

            @Override
            public PublicKey parsePublicKey(Map<String, Object> key) {
                // check 'data'
                if (key.get("data") == null) {
                    // key.data should not be empty
                    assert false : "ECC key error: " + key;
                    return null;
                }
                try {
                    return new ECCPublicKey(key);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        PublicKey.setFactory(AsymmetricAlgorithms.ECC, eccPub);
        PublicKey.setFactory(CryptoUtils.ECDSA_SHA256, eccPub);

    }

}
