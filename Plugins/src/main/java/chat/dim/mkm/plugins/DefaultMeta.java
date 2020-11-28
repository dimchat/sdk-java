/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.mkm.plugins;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.format.UTF8;
import chat.dim.mkm.BaseMeta;
import chat.dim.mkm.Identifier;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.MetaType;

/**
 *  Default Meta to build ID with 'name@address'
 *
 *  version:
 *      0x01 - MKM
 *
 *  algorithm:
 *      CT      = fingerprint = sKey.sign(seed);
 *      hash    = ripemd160(sha256(CT));
 *      code    = sha256(sha256(network + hash)).prefix(4);
 *      address = base58_encode(network + hash + code);
 *      number  = uint(code);
 */
public final class DefaultMeta extends BaseMeta {

    public DefaultMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public DefaultMeta(int version, VerifyKey key, String seed, byte[] fingerprint) {
        super(version, key, seed, fingerprint);
    }

    @Override
    public boolean matches(ID identifier) {
        if (identifier == null) {
            return false;
        }
        Address address = identifier.getAddress();
        if (address instanceof BTCAddress) {
            byte network = address.getNetwork();
            return identifier.equals(generateID(network));
        }
        return false;
    }

    // cache
    private Map<Byte, ID> cachedIdentifiers = new HashMap<>();

    public ID generateID(byte network) {
        // check cache
        ID identifier = cachedIdentifiers.get(network);
        if (identifier == null) {
            // generate and cache it
            identifier = new Identifier(getSeed(), generateAddress(network));
            cachedIdentifiers.put(network, identifier);
        }
        return identifier;
    }

    private Address generateAddress(byte network) {
        assert MetaType.MKM.equals(getType()) : "meta version error";
        if (!isValid()) {
            throw new IllegalArgumentException("meta invalid: " + getMap());
        }
        return BTCAddress.generate(getFingerprint(), network);
    }

    /**
     *  Generate meta with private key
     *
     * @param sKey - private key
     * @param seed - ID.name
     * @return Meta
     */
    public static DefaultMeta generate(PrivateKey sKey, String seed) {
        if (seed == null || seed.length() == 0) {
            throw new NullPointerException("default meta's seed should not be empty!");
        }
        int version = MetaType.MKM.value;
        byte[] fingerprint = sKey.sign(UTF8.encode(seed));
        return new DefaultMeta(version, sKey.getPublicKey(), seed, fingerprint);
    }
}
