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

import java.util.HashMap;
import java.util.Map;

import chat.dim.Address;
import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.crypto.PublicKey;
import chat.dim.protocol.MetaType;

/**
 *  Meta to build BTC address for ID
 *
 *  version:
 *      0x02 - BTC
 *      0x03 - ExBTC
 *
 *  algorithm:
 *      CT      = key.data;
 *      hash    = ripemd160(sha256(CT));
 *      code    = sha256(sha256(network + hash)).prefix(4);
 *      address = base58_encode(network + hash + code);
 *      number  = uint(code);
 */
public final class BTCMeta extends Meta {

    public BTCMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    // memory cache
    private Map<Byte, ID> idMap = new HashMap<>();

    @Override
    public ID generateID(byte network) {
        // check cache
        ID identifier = idMap.get(network);
        if (identifier == null) {
            // generate and cache it
            identifier = super.generateID(network);
            assert identifier.isValid() : "failed to generate ID: " + this;
            idMap.put(network, identifier);
        }
        return identifier;
    }

    @Override
    protected Address generateAddress(byte network) {
        assert MetaType.BTC.equals(getVersion()) || MetaType.ExBTC.equals(getVersion()) : "meta version error";
        if (!isValid()) {
            throw new IllegalArgumentException("meta invalid: " + dictionary);
        }
        // check cache
        ID identifier = idMap.get(network);
        if (identifier != null) {
            return identifier.address;
        }
        PublicKey key = getKey();
        byte[] data = key.getData();
        // generate
        return BTCAddress.generate(data, network);
    }
}
