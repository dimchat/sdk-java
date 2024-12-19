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

import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.AESKey;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PlainKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.digest.DataDigester;
import chat.dim.digest.MD5;
import chat.dim.digest.SHA1;
import chat.dim.digest.SHA256;
import chat.dim.format.Base58;
import chat.dim.format.Base64;
import chat.dim.format.Base64Data;
import chat.dim.format.BaseNetworkFile;
import chat.dim.format.DataCoder;
import chat.dim.format.Hex;
import chat.dim.format.HexCoder;
import chat.dim.format.PortableNetworkFile;
import chat.dim.format.StringCoder;
import chat.dim.format.TransportableData;
import chat.dim.format.UTF8;
import chat.dim.mkm.BTCAddress;
import chat.dim.mkm.BaseAddressFactory;
import chat.dim.mkm.ETHAddress;
import chat.dim.mkm.GeneralDocumentFactory;
import chat.dim.mkm.GeneralMetaFactory;
import chat.dim.mkm.IdentifierFactory;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class PluginLoader {

    private boolean isLoaded = false;

    /**
     *  Register core factories
     */
    public boolean load() {
        if (isLoaded) {
            // already loaded
            return false;
        } else {
            isLoaded = true;
        }

        registerDataCoders();
        registerDataDigesters();

        registerSymmetricKeyFactories();

        registerIDFactory();
        registerAddressFactory();
        registerMetaFactories();
        registerDocumentFactories();

        // OK
        return true;
    }

    /**
     *  Data coders
     */
    private void registerDataCoders() {

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

        // Base64 coding
        Base64.coder = new DataCoder() {

            @Override
            public String encode(byte[] data) {
                return java.util.Base64.getEncoder().encodeToString(data);
            }

            @Override
            public byte[] decode(String string) {
                return java.util.Base64.getDecoder().decode(string);
            }
        };

        // HEX coding
        Hex.coder = new HexCoder();

        // UTF8
        UTF8.coder = new StringCoder() {

            @SuppressWarnings("CharsetObjectCanBeUsed")
            @Override
            public byte[] encode(String string) {
                return string.getBytes(Charset.forName("UTF-8"));
            }

            @SuppressWarnings("CharsetObjectCanBeUsed")
            @Override
            public String decode(byte[] utf8) {
                return new String(utf8, Charset.forName("UTF-8"));
            }
        };

        // PNF
        PortableNetworkFile.setFactory(new PortableNetworkFile.Factory() {

            @Override
            public PortableNetworkFile createPortableNetworkFile(TransportableData data, String filename,
                                                                 URI url, DecryptKey key) {
                return new BaseNetworkFile(data, filename, url, key);
            }

            @Override
            public PortableNetworkFile parsePortableNetworkFile(Map<String, Object> pnf) {
                return new BaseNetworkFile(pnf);
            }
        });

        // TED
        TransportableData.Factory tedFactory = new TransportableData.Factory() {

            @Override
            public TransportableData createTransportableData(byte[] data) {
                return new Base64Data(data);
            }

            @Override
            public TransportableData parseTransportableData(Map<String, Object> ted) {
                // TODO: 1. check algorithm
                //       2. check data format
                return new Base64Data(ted);
            }
        };
        TransportableData.setFactory(TransportableData.BASE_64, tedFactory);
        //TransportableData.setFactory(TransportableData.DEFAULT, tedFactory);
        TransportableData.setFactory("*", tedFactory);
    }

    /**
     *  Data digesters
     */
    private void registerDataDigesters() {

        // MD5
        MD5.digester = new DataDigester() {

            @Override
            public byte[] digest(byte[] data) {
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
                md.reset();
                md.update(data);
                return md.digest();
            }
        };

        // SHA1
        SHA1.digester = new DataDigester() {

            @Override
            public byte[] digest(byte[] data) {
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
                md.reset();
                md.update(data);
                return md.digest();
            }
        };

        // SHA256
        SHA256.digester = new DataDigester() {

            @Override
            public byte[] digest(byte[] data) {
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return null;
                }
                md.reset();
                md.update(data);
                return md.digest();
            }
        };
    }

    /**
     *  Symmetric key parsers
     */
    private void registerSymmetricKeyFactories() {

        SymmetricKey.setFactory(SymmetricKey.AES, new SymmetricKey.Factory() {

            @Override
            public SymmetricKey generateSymmetricKey() {
                Map<String, Object> key = new HashMap<>();
                key.put("algorithm", SymmetricKey.AES);
                return parseSymmetricKey(key);
            }

            @Override
            public SymmetricKey parseSymmetricKey(Map<String, Object> key) {
                return new AESKey(key);
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

    /**
     *  ID factory
     */
    private void registerIDFactory() {

        ID.setFactory(new IdentifierFactory());
    }

    /**
     *  Address factory
     */
    private void registerAddressFactory() {

        Address.setFactory(new BaseAddressFactory() {
            @Override
            public Address createAddress(String address) {
                if (address == null) {
                    //throw new NullPointerException("address empty");
                    assert false : "address empty";
                    return null;
                }
                int len = address.length();
                if (len == 0) {
                    assert false : "address empty";
                    return null;
                } else if (len == 8) {
                    // "anywhere"
                    if (Address.ANYWHERE.equalsIgnoreCase(address)) {
                        return Address.ANYWHERE;
                    }
                } else if (len == 10) {
                    // "everywhere"
                    if (Address.EVERYWHERE.equalsIgnoreCase(address)) {
                        return Address.EVERYWHERE;
                    }
                }
                Address res;
                if (26 <= len && len <= 35) {
                    // BTC
                    res = BTCAddress.parse(address);
                } else if (len == 42) {
                    // ETH
                    res = ETHAddress.parse(address);
                } else {
                    //throw new AssertionError("invalid address: " + address);
                    assert false : "invalid address: " + address;
                    res = null;
                }
                // TODO: other types of address
                assert res != null : "invalid address: " + address;
                return res;
            }
        });
    }

    /**
     *  Meta factories
     */
    private void registerMetaFactories() {

        Meta.setFactory(Meta.MKM, new GeneralMetaFactory(Meta.MKM));
        Meta.setFactory(Meta.BTC, new GeneralMetaFactory(Meta.BTC));
        Meta.setFactory(Meta.ETH, new GeneralMetaFactory(Meta.ETH));
    }

    /**
     *  Document factories
     */
    private void registerDocumentFactories() {

        Document.setFactory("*", new GeneralDocumentFactory("*"));
        Document.setFactory(Document.VISA, new GeneralDocumentFactory(Document.VISA));
        Document.setFactory(Document.PROFILE, new GeneralDocumentFactory(Document.PROFILE));
        Document.setFactory(Document.BULLETIN, new GeneralDocumentFactory(Document.BULLETIN));
    }

}
