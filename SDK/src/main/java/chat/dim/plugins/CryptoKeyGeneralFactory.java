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

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.type.Converter;
import chat.dim.type.Wrapper;

/**
 *  CryptographyKey GeneralFactory
 */
public class CryptoKeyGeneralFactory implements GeneralCryptoHelper,
                                                SymmetricKeyHelper,
                                                PrivateKeyHelper, PublicKeyHelper {

    private final Map<String, SymmetricKey.Factory> symmetricKeyFactories = new HashMap<>();
    private final Map<String, PrivateKey.Factory>     privateKeyFactories = new HashMap<>();
    private final Map<String, PublicKey.Factory>       publicKeyFactories = new HashMap<>();

    @Override
    public String getKeyAlgorithm(Map<?, ?> key, String defaultValue) {
        return Converter.getString(key.get("algorithm"), defaultValue);
    }

    //
    //  SymmetricKey Helper
    //

    @Override
    public void setSymmetricKeyFactory(String algorithm, SymmetricKey.Factory factory) {
        symmetricKeyFactories.put(algorithm, factory);
    }

    @Override
    public SymmetricKey.Factory getSymmetricKeyFactory(String algorithm) {
        return symmetricKeyFactories.get(algorithm);
    }

    @Override
    public SymmetricKey generateSymmetricKey(String algorithm) {
        SymmetricKey.Factory factory = getSymmetricKeyFactory(algorithm);
        assert factory != null : "key algorithm not support: " + algorithm;
        return factory.generateSymmetricKey();
    }

    @Override
    public SymmetricKey parseSymmetricKey(Object key) {
        if (key == null) {
            return null;
        } else if (key instanceof SymmetricKey) {
            return (SymmetricKey) key;
        }
        Map<String, Object> info = Wrapper.getMap(key);
        if (info == null) {
            assert false : "symmetric key error: " + key;
            return null;
        }
        String algo = getKeyAlgorithm(info, null);
        // assert algo != null : "symmetric key error: " + key;
        SymmetricKey.Factory factory = algo == null ? null : getSymmetricKeyFactory(algo);
        if (factory == null) {
            // unknown algorithm, get default key factory
            factory = getSymmetricKeyFactory("*");  // unknown
            if (factory == null) {
                assert false : "default symmetric key factory not found: " + key;
                return null;
            }
        }
        return factory.parseSymmetricKey(info);
    }

    //
    //  PrivateKey Helper
    //

    @Override
    public void setPrivateKeyFactory(String algorithm, PrivateKey.Factory factory) {
        privateKeyFactories.put(algorithm, factory);
    }

    @Override
    public PrivateKey.Factory getPrivateKeyFactory(String algorithm) {
        return privateKeyFactories.get(algorithm);
    }

    @Override
    public PrivateKey generatePrivateKey(String algorithm) {
        PrivateKey.Factory factory = getPrivateKeyFactory(algorithm);
        assert factory != null : "key algorithm not support: " + algorithm;
        return factory.generatePrivateKey();
    }

    @Override
    public PrivateKey parsePrivateKey(Object key) {
        if (key == null) {
            return null;
        } else if (key instanceof PrivateKey) {
            return (PrivateKey) key;
        }
        Map<String, Object> info = Wrapper.getMap(key);
        if (info == null) {
            assert false : "private key error: " + key;
            return null;
        }
        String algo = getKeyAlgorithm(info, null);
        // assert algo != null : "private key error: " + key;
        PrivateKey.Factory factory = algo == null ? null : getPrivateKeyFactory(algo);
        if (factory == null) {
            // unknown algorithm, get default key factory
            factory = getPrivateKeyFactory("*");  // unknown
            if (factory == null) {
                assert false : "default private key factory not found: " + key;
                return null;
            }
        }
        return factory.parsePrivateKey(info);
    }

    //
    //  PublicKey Helper
    //

    @Override
    public void setPublicKeyFactory(String algorithm, PublicKey.Factory factory) {
        publicKeyFactories.put(algorithm, factory);
    }

    @Override
    public PublicKey.Factory getPublicKeyFactory(String algorithm) {
        return publicKeyFactories.get(algorithm);
    }

    @Override
    public PublicKey parsePublicKey(Object key) {
        if (key == null) {
            return null;
        } else if (key instanceof PublicKey) {
            return (PublicKey) key;
        }
        Map<String, Object> info = Wrapper.getMap(key);
        if (info == null) {
            assert false : "pubic key error: " + key;
            return null;
        }
        String algo = getKeyAlgorithm(info, null);
        // assert algo != null : "public key error: " + key;
        PublicKey.Factory factory = algo == null ? null : getPublicKeyFactory(algo);
        if (factory == null) {
            // unknown algorithm, get default key factory
            factory = getPublicKeyFactory("*");  // unknown
            if (factory == null) {
                assert false : "default public key factory not found: " + key;
                return null;
            }
        }
        return factory.parsePublicKey(info);
    }

}
