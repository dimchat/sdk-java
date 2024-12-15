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
package chat.dim.compat;

import java.util.Map;

import chat.dim.crypto.VerifyKey;
import chat.dim.format.TransportableData;
import chat.dim.mkm.AccountFactoryManager;
import chat.dim.mkm.BTCMeta;
import chat.dim.mkm.DefaultMeta;
import chat.dim.mkm.ETHMeta;
import chat.dim.mkm.GeneralMetaFactory;
import chat.dim.protocol.Meta;

public final class CompatibleMetaFactory extends GeneralMetaFactory {

    public CompatibleMetaFactory(String algorithm) {
        super(algorithm);
    }

    @Override
    public Meta createMeta(VerifyKey key, String seed, TransportableData fingerprint) {
        Meta out;
        switch (type) {

            case Meta.MKM:
                out = new DefaultMeta("1", key, seed, fingerprint);
                break;

            case Meta.BTC:
                out = new BTCMeta("2", key);
                break;

            case Meta.ETH:
                out = new ETHMeta("4", key);
                break;

            default:
                throw new IllegalArgumentException("unknown meta type: " + type);
        }
        assert out.isValid() : "meta error: " + out;
        return out;
    }

    @Override
    public Meta parseMeta(Map<String, Object> meta) {
        Meta out;
        AccountFactoryManager man = AccountFactoryManager.getInstance();
        String type = man.generalFactory.getMetaType(meta, "");
        switch (type) {

            case "MKM":
            case "mkm":
            case "1":
                out = new DefaultMeta(meta);
                break;

            case "BTC":
            case "btc":
            case "2":
                out = new BTCMeta(meta);
                break;

            case "ETH":
            case "eth":
            case "4":
                out = new ETHMeta(meta);
                break;

            default:
                throw new IllegalArgumentException("unknown meta type: " + type);
        }
        return out.isValid() ? out : null;
    }
}
