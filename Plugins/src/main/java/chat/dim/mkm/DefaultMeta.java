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
package chat.dim.mkm;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.VerifyKey;
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
 */
final class DefaultMeta extends BaseMeta {

    DefaultMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    DefaultMeta(int version, VerifyKey key, String seed, byte[] fingerprint) {
        super(version, key, seed, fingerprint);
    }

    // caches
    private final Map<Byte, Address> cachedAddresses = new HashMap<>();

    @Override
    public Address generateAddress(byte type) {
        assert MetaType.MKM.equals(getType()) : "meta version error: " + getType();
        // check caches
        Address address = cachedAddresses.get(type);
        if (address == null && isValid()) {
            // generate and cache it
            address = BTCAddress.generate(getFingerprint(), type);
            cachedAddresses.put(type, address);
        }
        return address;
    }


    @Override
    public boolean matches(ID identifier) {
        if (identifier.getAddress() instanceof BTCAddress) {
            return super.matches(identifier);
        }
        return false;
    }
}
