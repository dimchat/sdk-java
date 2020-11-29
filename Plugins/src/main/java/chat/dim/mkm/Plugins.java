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

import java.util.Map;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.format.UTF8;
import chat.dim.mkm.plugins.BTCAddress;
import chat.dim.mkm.plugins.BTCMeta;
import chat.dim.mkm.plugins.DefaultMeta;
import chat.dim.mkm.plugins.ETHAddress;
import chat.dim.mkm.plugins.ETHMeta;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;

public abstract class Plugins extends chat.dim.crypto.Plugins {

    static {

        /*
         *  Register classes
         */
        // JsON
        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        serializeConfig.put(Address.class, ToStringSerializer.instance);
        serializeConfig.put(ID.class, ToStringSerializer.instance);

        Factories.addressFactory = new AddressFactory() {

            @Override
            protected Address createAddress(String string) {
                int len = string.length();
                if (len == 42) {
                    return ETHAddress.parse(string);
                }
                return BTCAddress.parse(string);
            }
        };
        Factories.metaFactory = new Meta.Factory() {

            @Override
            public Meta createMeta(int type, VerifyKey key, String seed, byte[] fingerprint) {
                if (MetaType.Default.equals(type)) {
                    return new DefaultMeta(type, key, seed, fingerprint);
                } else if (MetaType.BTC.equals(type) || MetaType.ExBTC.equals(type)) {
                    return new BTCMeta(type, key, seed, fingerprint);
                } else if (MetaType.ETH.equals(type) || MetaType.ExETH.equals(type)) {
                    return new ETHMeta(type, key, seed, fingerprint);
                }
                return null;
            }

            @Override
            public Meta generateMeta(int type, SignKey sKey, String seed) {
                byte[] fingerprint;
                if (seed == null || seed.length() == 0) {
                    fingerprint = null;
                } else {
                    fingerprint = sKey.sign(UTF8.encode(seed));
                }
                VerifyKey key = ((PrivateKey) sKey).getPublicKey();
                return createMeta(type, key, seed, fingerprint);
            }

            @Override
            public Meta parseMeta(Map<String, Object> meta) {
                Object version = meta.get("version");
                if (version == null) {
                    version = meta.get("type");
                }
                int type = (int) version;
                if (MetaType.Default.equals(type)) {
                    return new DefaultMeta(meta);
                } else if (MetaType.BTC.equals(type) || MetaType.ExBTC.equals(type)) {
                    return new BTCMeta(meta);
                } else if (MetaType.ETH.equals(type) || MetaType.ExETH.equals(type)) {
                    return new ETHMeta(meta);
                }
                return null;
            }
        };
        Factories.documentFactory = new Document.Factory() {

            @Override
            public Document createDocument(ID identifier, String type, String data, String signature) {
                if (ID.isUser(identifier)) {
                    if (type == null || Document.VISA.equals(type)) {
                        return new UserProfile(identifier, data, signature);
                    }
                } else if (ID.isGroup(identifier)) {
                    return new BaseBulletin(identifier, data, signature);
                }
                return new BaseDocument(identifier, data, signature);
            }

            @Override
            public Document generateDocument(ID identifier, String type) {
                if (ID.isUser(identifier)) {
                    if (type == null || Document.VISA.equals(type)) {
                        return new UserProfile(identifier);
                    }
                } else if (ID.isGroup(identifier)) {
                    return new BaseBulletin(identifier);
                }
                return new BaseDocument(identifier, type);
            }

            @Override
            public Document parseDocument(Map<String, Object> doc) {
                ID identifier = ID.parse(doc.get("ID"));
                if (identifier == null) {
                    return null;
                }
                if (ID.isUser(identifier)) {
                    String type = (String) doc.get("type");
                    if (type == null || Document.VISA.equals(type)) {
                        return new UserProfile(doc);
                    }
                } else if (ID.isGroup(identifier)) {
                    return new BaseBulletin(doc);
                }
                return new BaseDocument(doc);
            }
        };
    }
}
