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
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;

public abstract class Plugins extends chat.dim.crypto.Plugins {

    static class MetaFactory implements Meta.Factory {

        private final int version;

        MetaFactory(MetaType type) {
            super();
            version = type.value;
        }

        @Override
        public Meta createMeta(VerifyKey key, String seed, byte[] fingerprint) {
            if (MetaType.Default.equals(version)) {
                // MKM
                return new DefaultMeta(version, key, seed, fingerprint);
            } else if (MetaType.BTC.equals(version) || MetaType.ExBTC.equals(version)) {
                // BTC, ExBTC
                return new BTCMeta(version, key, seed, fingerprint);
            } else if (MetaType.ETH.equals(version) || MetaType.ExETH.equals(version)) {
                // ETH, ExETH
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
            int type = Meta.getType(meta);
            if (MetaType.Default.equals(type)) {
                // MKM
                return new DefaultMeta(meta);
            } else if (MetaType.BTC.equals(type) || MetaType.ExBTC.equals(type)) {
                // BTC, ExBTC
                return new BTCMeta(meta);
            } else if (MetaType.ETH.equals(type) || MetaType.ExETH.equals(type)) {
                // ETH, ExETH
                return new ETHMeta(meta);
            }
            return null;
        }
    }

    static class DocumentFactory implements Document.Factory {

        private final String version;

        DocumentFactory(String type) {
            super();
            version = type;
        }

        private String getType(ID identifier) {
            if (version.equals("*")) {
                if (ID.isGroup(identifier)) {
                    return Document.BULLETIN;
                }
                if (ID.isUser(identifier)) {
                    return Document.VISA;
                }
                return Document.PROFILE;
            }
            return version;
        }

        @Override
        public Document createDocument(ID identifier, byte[] data, byte[] signature) {
            String type = getType(identifier);
            if (Document.VISA.equals(type)) {
                return new BaseVisa(identifier, data, signature);
            }
            if (Document.BULLETIN.equals(type)) {
                return new BaseBulletin(identifier, data, signature);
            }
            return new BaseDocument(identifier, data, signature);
        }

        @Override
        public Document createDocument(ID identifier) {
            String type = getType(identifier);
            if (Document.VISA.equals(type)) {
                return new BaseVisa(identifier);
            }
            if (Document.BULLETIN.equals(type)) {
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
            String type = Document.getType(doc);
            if (type == null) {
                if (ID.isGroup(identifier)) {
                    type = Document.BULLETIN;
                } else {
                    type = Document.VISA;
                }
            }
            if (Document.VISA.equals(type)) {
                return new BaseVisa(doc);
            }
            if (Document.BULLETIN.equals(type)) {
                return new BaseBulletin(doc);
            }
            return new BaseDocument(doc);
        }
    }

    static {

        /*
         *  JsON
         */
        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        serializeConfig.put(Address.class, ToStringSerializer.instance);
        serializeConfig.put(ID.class, ToStringSerializer.instance);

        /*
         *  Address factory
         */
        Address.setFactory(new AddressFactory() {
            @Override
            protected Address createAddress(String address) {
                int len = address.length();
                if (len == 42) {
                    return ETHAddress.parse(address);
                }
                return BTCAddress.parse(address);
            }
        });

        /*
         *  Meta factories
         */
        Meta.register(MetaType.MKM, new MetaFactory(MetaType.MKM));
        Meta.register(MetaType.BTC, new MetaFactory(MetaType.BTC));
        Meta.register(MetaType.ExBTC, new MetaFactory(MetaType.ExBTC));
        Meta.register(MetaType.ETH, new MetaFactory(MetaType.ETH));
        Meta.register(MetaType.ExETH, new MetaFactory(MetaType.ExETH));

        /*
         *  Document factories
         */
        Document.register("*", new DocumentFactory("*"));
        Document.register(Document.VISA, new DocumentFactory(Document.VISA));
        Document.register(Document.PROFILE, new DocumentFactory(Document.PROFILE));
        Document.register(Document.BULLETIN, new DocumentFactory(Document.BULLETIN));
    }
}
