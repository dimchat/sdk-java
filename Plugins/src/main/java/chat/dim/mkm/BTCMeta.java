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
package chat.dim.mkm;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.VerifyKey;
import chat.dim.format.TransportableData;
import chat.dim.protocol.Address;

/**
 *  Meta to build BTC address for ID
 *
 *  <blockquote><pre>
 *  version:
 *      2 = BTC
 *
 *  algorithm:
 *      CT      = key.data;
 *      hash    = ripemd160(sha256(CT));
 *      code    = sha256(sha256(network + hash)).prefix(4);
 *      address = base58_encode(network + hash + code);
 *  </pre></blockquote>
 */
public final class BTCMeta extends BaseMeta {

    public BTCMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public BTCMeta(String type, VerifyKey key) {
        super(type, key, null, null);
    }

    public BTCMeta(String type, VerifyKey key, String seed, TransportableData fingerprint) {
        super(type, key, seed, fingerprint);
    }

    @Override
    protected boolean hasSeed() {
        return false;
    }

    // caches
    private final Map<Byte, Address> cachedAddresses = new HashMap<>();

    @Override
    public Address generateAddress(int type) {
        //assert Meta.BTC.equals(getType()) || "2".equals(getType()) : "meta version error: " + getType();
        byte network = (byte) type;
        // check caches
        Address cached = cachedAddresses.get(network);
        if (cached == null) {
            // TODO: compress public key?
            VerifyKey key = getPublicKey();
            byte[] data = key.getData();
            // generate and cache it
            cached = BTCAddress.generate(data, network);
            cachedAddresses.put(network, cached);
        }
        return cached;
    }
}
