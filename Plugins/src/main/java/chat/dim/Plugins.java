/* license: https://mit-license.org
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
package chat.dim;

import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.core.AddressFactory;
import chat.dim.crypto.AESKey;
import chat.dim.crypto.PlainKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base58;
import chat.dim.format.DataCoder;
import chat.dim.format.Hex;
import chat.dim.format.HexCoder;
import chat.dim.mkm.BTCAddress;
import chat.dim.mkm.DocumentFactory;
import chat.dim.mkm.ETHAddress;
import chat.dim.mkm.EntityIDFactory;
import chat.dim.mkm.MetaFactory;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;

public interface Plugins {

    static void registerDataCoders() {

        // Base58 coding
        Base58.coder = new DataCoder() {

            @Override
            public String encode(byte[] data) {
                return chat.dim.bitcoinj.Base58.encode(data);
            }

            @Override
            public byte[] decode(String string) {
                return chat.dim.bitcoinj.Base58.decode(string);
            }
        };

        // HEX coding
        Hex.coder = new HexCoder();

    }

    /*
     *  Symmetric Key Parsers
     */
    static void registerSymmetricKeyFactories() {

        SymmetricKey.setFactory(SymmetricKey.AES, new SymmetricKey.Factory() {

            @Override
            public SymmetricKey generateSymmetricKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", SymmetricKey.AES);
                return parseSymmetricKey(key);
            }

            @Override
            public SymmetricKey parseSymmetricKey(Map<String, Object> key) {
                try {
                    return new AESKey(key);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        SymmetricKey.setFactory(PlainKey.PLAIN, new SymmetricKey.Factory() {

            @Override
            public SymmetricKey generateSymmetricKey() {
                return PlainKey.getInstance();
            }

            @Override
            public SymmetricKey parseSymmetricKey(Map<String, Object> key) {
                return PlainKey.getInstance();
            }
        });
    }

    /*
     *  ID factory
     */
    static void registerIDFactory() {

        ID.setFactory(new EntityIDFactory());
    }

    /*
     *  Address factory
     */
    static void registerAddressFactory() {

        Address.setFactory(new AddressFactory() {
            @Override
            public Address createAddress(String address) {
                int len = address.length();
                if (len == 8 && address.equalsIgnoreCase("anywhere")) {
                    return Address.ANYWHERE;
                } else if (len == 10 && address.equalsIgnoreCase("everywhere")) {
                    return Address.EVERYWHERE;
                } else if (len == 42) {
                    return ETHAddress.parse(address);
                } else if (26 <= len && len <= 35) {
                    return BTCAddress.parse(address);
                }
                throw new AssertionError("invalid address: " + address);
            }
        });
    }

    /*
     *  Meta factories
     */
    static void registerMetaFactories() {

        Meta.setFactory(MetaType.MKM, new MetaFactory(MetaType.MKM));
        Meta.setFactory(MetaType.BTC, new MetaFactory(MetaType.BTC));
        Meta.setFactory(MetaType.ExBTC, new MetaFactory(MetaType.ExBTC));
        Meta.setFactory(MetaType.ETH, new MetaFactory(MetaType.ETH));
        Meta.setFactory(MetaType.ExETH, new MetaFactory(MetaType.ExETH));
    }

    /*
     *  Document factories
     */
    static void registerDocumentFactories() {

        Document.setFactory("*", new DocumentFactory("*"));
        Document.setFactory(Document.VISA, new DocumentFactory(Document.VISA));
        Document.setFactory(Document.PROFILE, new DocumentFactory(Document.PROFILE));
        Document.setFactory(Document.BULLETIN, new DocumentFactory(Document.BULLETIN));
    }

    static void registerPlugins() {

        registerDataCoders();

        registerSymmetricKeyFactories();

        registerIDFactory();
        registerAddressFactory();
        registerMetaFactories();
        registerDocumentFactories();
    }
}
