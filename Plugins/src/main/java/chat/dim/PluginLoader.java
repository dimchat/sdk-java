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
import chat.dim.mkm.BaseAddressFactory;
import chat.dim.mkm.GeneralDocumentFactory;
import chat.dim.mkm.GeneralMetaFactory;
import chat.dim.mkm.IdentifierFactory;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class PluginLoader implements Runnable {

    private boolean loaded = false;

    @Override
    public void run() {
        if (loaded) {
            // no need to load it again
            return;
        } else {
            // mark it to loaded
            loaded = true;
        }
        try {
            load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Register core factories
     */
    protected void load() {

        registerDataCoders();
        registerDataDigesters();

        registerSymmetricKeyFactories();

        registerIDFactory();
        registerAddressFactory();
        registerMetaFactories();
        registerDocumentFactories();

    }

    /**
     *  Data coders
     */
    protected void registerDataCoders() {

        registerBase58Coder();
        registerBase64Coder();

        registerHexCoder();

        registerUTF8Coder();

        registerPNFFactory();

        registerTEDFactory();

    }
    protected void registerBase58Coder() {
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
    }
    protected void registerBase64Coder() {
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
    }
    protected void registerHexCoder() {
        // HEX coding
        Hex.coder = new HexCoder();
    }
    protected void registerUTF8Coder() {
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
    }
    protected void registerPNFFactory() {
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
    }
    protected void registerTEDFactory() {
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
    protected void registerDataDigesters() {

        registerMD5Digester();

        registerSHA1Digester();

        registerSHA256Digester();

    }
    protected void registerMD5Digester() {
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
    }
    protected void registerSHA1Digester() {
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
    }
    protected void registerSHA256Digester() {
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
    protected void registerSymmetricKeyFactories() {

        registerAESKeyFactory();

        registerPlainKeyFactory();

    }
    protected void registerAESKeyFactory() {
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
    }
    protected void registerPlainKeyFactory() {
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
    protected void registerIDFactory() {

        ID.setFactory(new IdentifierFactory());
    }

    /**
     *  Address factory
     */
    protected void registerAddressFactory() {

        Address.setFactory(new BaseAddressFactory());
    }

    /**
     *  Meta factories
     */
    protected void registerMetaFactories() {

        Meta.setFactory(Meta.MKM, new GeneralMetaFactory(Meta.MKM));
        Meta.setFactory(Meta.BTC, new GeneralMetaFactory(Meta.BTC));
        Meta.setFactory(Meta.ETH, new GeneralMetaFactory(Meta.ETH));
    }

    /**
     *  Document factories
     */
    protected void registerDocumentFactories() {

        Document.setFactory("*", new GeneralDocumentFactory("*"));
        Document.setFactory(Document.VISA, new GeneralDocumentFactory(Document.VISA));
        Document.setFactory(Document.PROFILE, new GeneralDocumentFactory(Document.PROFILE));
        Document.setFactory(Document.BULLETIN, new GeneralDocumentFactory(Document.BULLETIN));
    }

}
