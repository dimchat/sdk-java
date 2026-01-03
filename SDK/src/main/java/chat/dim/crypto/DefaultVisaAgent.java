/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2025 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Albert Moky
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
package chat.dim.crypto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chat.dim.protocol.Document;
import chat.dim.protocol.EncryptKey;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.PublicKey;
import chat.dim.protocol.VerifyKey;
import chat.dim.protocol.Visa;

public class DefaultVisaAgent implements VisaAgent {

    @Override
    public EncryptedBundle encryptBundle(byte[] plaintext, Meta meta, List<Document> documents) {
        // NOTICE: meta.key will never changed, so use visa.key to encrypt message
        //         is a better way
        EncryptedBundle bundle = new UserEncryptedBundle();
        String terminal;
        EncryptKey pubKey;
        byte[] ciphertext;
        //
        //  1. encrypt with visa keys
        //
        for (Document doc : documents) {
            // encrypt by public key
            pubKey = getEncryptKey(doc);
            if (pubKey == null) {
                continue;
            }
            // get visa.terminal
            terminal = getTerminal(doc);
            if (terminal == null || terminal.isEmpty()) {
                terminal = "*";
            }
            if (bundle.get(terminal) != null) {
                assert false : "duplicated visa key: " + doc;
                continue;
            }
            ciphertext = pubKey.encrypt(plaintext, null);
            bundle.put(terminal, ciphertext);
        }
        if (bundle.isEmpty()) {
            //
            //  2. encrypt with meta key
            //
            VerifyKey metaKey = meta.getPublicKey();
            if (metaKey instanceof EncryptKey) {
                pubKey = (EncryptKey) metaKey;
                //terminal = "*";
                ciphertext = pubKey.encrypt(plaintext, null);
                bundle.put("*", ciphertext);
            }
        }
        // OK
        return bundle;
    }

    @Override
    public List<VerifyKey> getVerifyKeys(Meta meta, List<Document> documents) {
        List<VerifyKey> keys = new ArrayList<>();
        VerifyKey pubKey;
        // the sender may use communication key to sign message.data,
        // try to verify it with visa.key first;
        for (Document doc : documents) {
            pubKey = getVerifyKey(doc);
            if (pubKey != null) {
                keys.add(pubKey);
            } else {
                assert false : "failed to get visa key: " + doc;
            }
        }
        // the sender may use identity key to sign message.data,
        // try to verify it with meta.key too.
        pubKey = meta.getPublicKey();
        if (pubKey != null) {
            keys.add(pubKey);
        } else {
            assert false : "failed to get meta key: " + meta;
        }
        // OK
        return keys;
    }

    protected VerifyKey getVerifyKey(Document doc) {
        if (doc instanceof Visa) {
            EncryptKey visaKey = ((Visa) doc).getPublicKey();
            if (visaKey instanceof VerifyKey) {
                return (VerifyKey) visaKey;
            }
            assert false : "failed to get visa key: " + doc;
            return null;
        }
        // public key in user profile?
        return PublicKey.parse(doc.getProperty("key"));
    }

    protected EncryptKey getEncryptKey(Document doc) {
        if (doc instanceof Visa) {
            EncryptKey visaKey = ((Visa) doc).getPublicKey();
            if (visaKey != null) {
                return visaKey;
            }
            assert false : "failed to get visa key: " + doc;
            return null;
        }
        PublicKey pubKey = PublicKey.parse(doc.getProperty("key"));
        if (pubKey == null) {
            // profile document?
            return null;
        } else if (pubKey instanceof EncryptKey) {
            return (EncryptKey) pubKey;
        }
        assert false : "visa key error: " + pubKey;
        return null;
    }

    protected String getTerminal(Document doc) {
        String terminal = doc.getString("terminal");
        if (terminal == null) {
            // get from document ID
            ID did = ID.parse(doc.get("did"));
            if (did != null) {
                terminal = did.getTerminal();
            } else {
                assert false : "document ID not found: " + doc;
                // TODO: get from property?
            }
        }
        return terminal;
    }

    @Override
    public Set<String> getTerminals(List<Document> documents) {
        Set<String> devices = new HashSet<>();
        String terminal;
        for (Document doc : documents) {
            terminal = getTerminal(doc);
            if (terminal == null || terminal.isEmpty()) {
                terminal = "*";
            }
            devices.add(terminal);
        }
        return devices;
    }

}
