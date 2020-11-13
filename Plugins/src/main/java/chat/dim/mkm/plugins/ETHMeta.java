/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.mkm.plugins;

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
 *  Meta to build ETH address for ID
 *
 *  version:
 *      0x04 - ETH
 *      0x05 - ExETH
 *
 *  algorithm:
 *          fingerprint = key.data;
 *          digest      = keccak256(fingerprint);
 *          address     = hex_encode(digest.suffix(20));
 */
public final class ETHMeta extends BaseMeta {

    public ETHMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public ETHMeta(int version, VerifyKey key, String seed, byte[] fingerprint) {
        super(version, key, seed, fingerprint);
    }

    @Override
    public boolean matches(ID identifier) {
        if (identifier == null) {
            return false;
        }
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            return identifier.equals(generateID());
        }
        return false;
    }

    // caches
    private ID cachedID = null;
    private Address cachedAddress = null;

    public ID generateID() {
        // check cache
        if (cachedID == null) {
            // generate and cache it
            cachedID = new Identifier(getSeed(), generateAddress());
        }
        return cachedID;
    }

    private Address generateAddress() {
        assert MetaType.ETH.equals(getType()) || MetaType.ExETH.equals(getType()) : "meta version error";
        if (!isValid()) {
            throw new IllegalArgumentException("meta invalid: " + getMap());
        }
        // check cache
        if (cachedAddress == null) {
            // generate and cache it
            VerifyKey key = getKey();
            byte[] data = key.getData();
            cachedAddress = ETHAddress.generate(data);
        }
        return cachedAddress;
    }

    /**
     *  Generate meta with private key
     *
     * @param sKey - private key
     * @param seed - ID.name
     * @return Meta
     */
    public static ETHMeta generate(PrivateKey sKey, String seed) {
        int version;
        byte[] fingerprint;
        if (seed == null || seed.length() == 0) {
            version = MetaType.ETH.value;
            fingerprint = null;
        } else {
            version = MetaType.ExETH.value;
            fingerprint = sKey.sign(UTF8.encode(seed));
        }
        return new ETHMeta(version, sKey.getPublicKey(), seed, fingerprint);
    }
}
