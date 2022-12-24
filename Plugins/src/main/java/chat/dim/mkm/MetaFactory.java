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

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.format.UTF8;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;

public final class MetaFactory implements Meta.Factory {

    private final int version;

    public MetaFactory(MetaType type) {
        super();
        version = type.value;
    }

    @Override
    public Meta createMeta(VerifyKey key, String seed, byte[] fingerprint) {
        if (MetaType.MKM.equals(version)) {
            // MKM
            return new DefaultMeta(version, key, seed, fingerprint);
        } else if (MetaType.BTC.equals(version)) {
            // BTC
            return new BTCMeta(version, key);
        } else if (MetaType.ExBTC.equals(version)) {
            // ExBTC
            return new BTCMeta(version, key, seed, fingerprint);
        } else if (MetaType.ETH.equals(version)) {
            // ETH
            return new ETHMeta(version, key);
        } else if (MetaType.ExETH.equals(version)) {
            // ExETH
            return new ETHMeta(version, key, seed, fingerprint);
        }
        return null;
    }

    @Override
    public Meta generateMeta(SignKey sKey, String seed) {
        byte[] fingerprint;
        if (seed == null || seed.length() == 0) {
            fingerprint = null;
        } else {
            fingerprint = sKey.sign(UTF8.encode(seed));
        }
        VerifyKey key = ((PrivateKey) sKey).getPublicKey();
        return createMeta(key, seed, fingerprint);
    }

    @Override
    public Meta parseMeta(Map<String, Object> meta) {
        Meta out;
        int type = Meta.getType(meta);
        if (MetaType.MKM.equals(type)) {
            // MKM
            out = new DefaultMeta(meta);
        } else if (MetaType.BTC.equals(type) || MetaType.ExBTC.equals(type)) {
            // BTC, ExBTC
            out = new BTCMeta(meta);
        } else if (MetaType.ETH.equals(type) || MetaType.ExETH.equals(type)) {
            // ETH, ExETH
            out = new ETHMeta(meta);
        } else {
            throw new IllegalArgumentException("unknown meta type: " + type);
        }
        return Meta.check(out) ? out : null;
    }
}
