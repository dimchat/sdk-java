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

import java.util.Map;

import chat.dim.crypto.VerifyKey;
import chat.dim.format.TransportableData;
import chat.dim.protocol.Address;
import chat.dim.protocol.EntityType;

/**
 *  Meta to build ETH address for ID
 *
 *  <blockquote><pre>
 *  version:
 *      4 = ETH
 *
 *  algorithm:
 *      CT      = key.data;  // without prefix byte
 *      digest  = keccak256(CT);
 *      address = hex_encode(digest.suffix(20));
 *  </pre></blockquote>
 */
public final class ETHMeta extends BaseMeta {

    public ETHMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public ETHMeta(String type, VerifyKey key) {
        super(type, key, null, null);
    }

    public ETHMeta(String type, VerifyKey key, String seed, TransportableData fingerprint) {
        super(type, key, seed, fingerprint);
    }

    @Override
    protected boolean hasSeed() {
        return false;
    }

    // cache
    private Address cachedAddress = null;

    @Override
    public Address generateAddress(int type) {
        //assert Meta.ETH.equals(getType()) || "4".equals(getType()) : "meta version error: " + getType();
        assert EntityType.USER.equals(type) : "ETH address type error: " + type;
        // check cache
        Address cached = cachedAddress;
        if (cached == null/* || cached.getType() != type*/) {
            // 64 bytes key data without prefix 0x04
            VerifyKey key = getPublicKey();
            byte[] data = key.getData();
            // generate and cache it
            cached = ETHAddress.generate(data);
            cachedAddress = cached;
        }
        return cached;
    }
}
