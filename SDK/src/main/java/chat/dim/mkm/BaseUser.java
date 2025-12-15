/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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

import java.util.List;
import java.util.Map;

import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.SignKey;
import chat.dim.protocol.VerifyKey;
import chat.dim.protocol.Visa;

public class BaseUser extends BaseEntity implements User {

    private final VisaAgent visaAgent;

    public BaseUser(ID uid) {
        super(uid);
        visaAgent = createVisaAgent();
    }

    protected VisaAgent createVisaAgent() {
        return new VisaAgent();
    }

    @Override
    public User.DataSource getDataSource() {
        Entity.DataSource facebook = super.getDataSource();
        if (facebook instanceof User.DataSource) {
            return (User.DataSource) facebook;
        }
        assert facebook == null : "user data source error: " + facebook;
        return null;
    }

    @Override
    public List<ID> getContacts() {
        User.DataSource facebook = getDataSource();
        if (facebook == null) {
            assert false : "user datasource not set yet";
            return null;
        }
        return facebook.getContacts(identifier);
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        Meta meta = getMeta();
        List<Document> documents = getDocuments();
        if (meta == null || documents == null) {
            assert false : "user not ready: " + identifier;
            return false;
        }
        assert !documents.isEmpty() : "documents empty: " + identifier;
        List<VerifyKey> keys = visaAgent.getVerifyKeys(meta, documents);
        if (keys == null) {
            assert false : "failed to get verify keys: " + identifier;
            return false;
        }
        assert !keys.isEmpty() : "failed to get verify keys: " + identifier;
        for (VerifyKey key : keys) {
            if (key.verify(data, signature)) {
                // matched!
                return true;
            }
        }
        // signature not match
        // TODO: check whether visa is expired, query new document for this contact
        return false;
    }

    @Override
    public Map<String, byte[]> encrypt(byte[] plaintext) {
        Meta meta = getMeta();
        List<Document> documents = getDocuments();
        if (meta == null || documents == null) {
            assert false : "user not ready: " + identifier;
            return null;
        }
        assert !documents.isEmpty() : "documents empty: " + identifier;
        return visaAgent.encrypt(plaintext, meta, documents);
    }

    //
    //  Interfaces for Local User
    //

    @Override
    public byte[] sign(byte[] data) {
        SignKey sKey = getPrivateKeyForSignature();
        if (sKey == null) {
            assert false : "failed to get sign key for user: " + identifier;
            return null;
        }
        return sKey.sign(data);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        // NOTICE: if you provide a public key in visa for encryption,
        //         here you should return the private key paired with visa.key
        List<DecryptKey> keys = getPrivateKeysForDecryption();
        if (keys == null) {
            assert false : "failed to get decrypt keys for user: " + identifier;
            return null;
        }
        assert !keys.isEmpty() : "failed to get decrypt keys for user: " + identifier;
        byte[] plaintext;
        for (DecryptKey key : keys) {
            // try decrypting it with each private key
            plaintext = key.decrypt(ciphertext, null);
            if (plaintext != null) {
                // OK!
                return plaintext;
            }
        }
        // decryption failed
        // TODO: check whether my visa key is changed, push new visa to this contact
        return null;
    }

    @Override
    public Visa sign(Visa doc) {
        ID did = ID.parse(doc.get("did"));
        assert did == null || identifier.getAddress().equals(did.getAddress()) : "visa ID not match: " + identifier + ", " + did;
        // NOTICE: only sign visa with the private key paired with your meta.key
        SignKey sKey = getPrivateKeyForVisaSignature();
        if (sKey == null) {
            assert false : "failed to get sign key for visa: " + did;
            return null;
        }
        if (doc.sign(sKey) == null) {
            assert false : "failed to sign visa: " + did + ", " + doc;
            return null;
        }
        return doc;
    }

    @Override
    public boolean verify(Visa doc) {
        // NOTICE: only verify visa with meta.key
        //         (if meta not exists, user won't be created)
        ID did = ID.parse(doc.get("did"));
        assert did == null || identifier.getAddress().equals(did.getAddress()) : "visa ID not match: " + identifier + ", " + did;
        Meta meta = getMeta();
        if (meta == null) {
            assert false : "failed to get meta: " + identifier;
            return false;
        }
        VerifyKey pKey = meta.getPublicKey();
        if (pKey == null) {
            assert false : "failed to get verify key for visa: " + did;
            return false;
        }
        return doc.verify(pKey);
    }

    //
    //  Private Keys
    //

    protected SignKey getPrivateKeyForSignature() {
        User.DataSource facebook = getDataSource();
        if (facebook == null) {
            assert false : "user datasource not set yet";
            return null;
        }
        return facebook.getPrivateKeyForSignature(identifier);
    }

    protected List<DecryptKey> getPrivateKeysForDecryption() {
        User.DataSource facebook = getDataSource();
        if (facebook == null) {
            assert false : "user datasource not set yet";
            return null;
        }
        return facebook.getPrivateKeysForDecryption(identifier);
    }

    protected SignKey getPrivateKeyForVisaSignature() {
        User.DataSource facebook = getDataSource();
        if (facebook == null) {
            assert false : "user datasource not set yet";
            return null;
        }
        return facebook.getPrivateKeyForVisaSignature(identifier);
    }

}
