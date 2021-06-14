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
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.NetworkType;

/**
 *  Meta to build ETH address for ID
 *
 *  version:
 *      0x04 - ETH
 *      0x05 - ExETH
 *
 *  algorithm:
 *      CT      = key.data;  // without prefix byte
 *      digest  = keccak256(CT);
 *      address = hex_encode(digest.suffix(20));
 */
final class ETHMeta extends BaseMeta {

    ETHMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    ETHMeta(int version, VerifyKey key) {
        super(version, key, null, null);
    }

    ETHMeta(int version, VerifyKey key, String seed, byte[] fingerprint) {
        super(version, key, seed, fingerprint);
    }

    // cache
    private Address cachedAddress = null;

    @Override
    public Address generateAddress(byte type) {
        assert NetworkType.MAIN.equals(type) : "ETH address type error: " + type;
        assert MetaType.ETH.equals(getType()) || MetaType.ExETH.equals(getType()) : "meta version error";
        if (cachedAddress == null && isValid()) {
            // generate and cache it
            VerifyKey key = getKey();
            byte[] data = key.getData();
            cachedAddress = ETHAddress.generate(data);
        }
        return cachedAddress;
    }

    @Override
    public boolean matches(ID identifier) {
        if (identifier.getAddress() instanceof ETHAddress) {
            return super.matches(identifier);
        }
        return false;
    }
}
