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
import chat.dim.format.TransportableData;
import chat.dim.format.UTF8;
import chat.dim.protocol.Meta;

public final class GeneralMetaFactory implements Meta.Factory {

    private final String algorithm;

    public GeneralMetaFactory(String type) {
        super();
        algorithm = type;
    }

    @Override
    public Meta createMeta(VerifyKey key, String seed, TransportableData fingerprint) {
        switch (algorithm) {

            case Meta.MKM:
            case "1":
                return new DefaultMeta("1", key, seed, fingerprint);

            case Meta.BTC:
            case "2":
                return new BTCMeta("2", key);

            case Meta.ETH:
            case "4":
                // ETH
                return new ETHMeta("4", key);
        }
        return null;
    }

    @Override
    public Meta generateMeta(SignKey sKey, String seed) {
        TransportableData fingerprint;
        if (seed == null || seed.length() == 0) {
            fingerprint = null;
        } else {
            byte[] sig = sKey.sign(UTF8.encode(seed));
            fingerprint = TransportableData.create(sig);
        }
        VerifyKey key = ((PrivateKey) sKey).getPublicKey();
        return createMeta(key, seed, fingerprint);
    }

    @Override
    public Meta parseMeta(Map<String, Object> meta) {
        Meta out;
        AccountFactoryManager man = AccountFactoryManager.getInstance();
        String type = man.generalFactory.getMetaType(meta, "");
        switch (type) {

            case Meta.MKM:
            case "1":
                out = new DefaultMeta(meta);
                break;

            case Meta.BTC:
            case "2":
                out = new BTCMeta(meta);
                break;

            case Meta.ETH:
            case "4":
                out = new ETHMeta(meta);
                break;

            default:
                throw new IllegalArgumentException("unknown meta type: " + type);
        }
        return out.isValid() ? out : null;
    }
}
