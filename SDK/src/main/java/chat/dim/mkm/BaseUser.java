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

import chat.dim.protocol.DecryptKey;
import chat.dim.protocol.Document;
import chat.dim.protocol.EncryptKey;
import chat.dim.protocol.ID;
import chat.dim.protocol.SignKey;
import chat.dim.protocol.VerifyKey;
import chat.dim.protocol.Visa;

public class BaseUser extends BaseEntity implements User {

    public BaseUser(ID uid) {
        super(uid);
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
    public Visa getVisa() {
        List<Document> documents = getDocuments();
        return DocumentUtils.lastVisa(documents);
    }

    @Override
    public List<ID> getContacts() {
        User.DataSource facebook = getDataSource();
        assert facebook != null : "user delegate not set yet";
        return facebook.getContacts(identifier);
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        User.DataSource facebook = getDataSource();
        assert facebook != null : "user delegate not set yet";
        List<VerifyKey> keys = facebook.getPublicKeysForVerification(identifier);
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
    public byte[] encrypt(byte[] plaintext) {
        User.DataSource facebook = getDataSource();
        assert facebook != null : "user delegate not set yet";
        // NOTICE: meta.key will never changed, so use visa.key to encrypt message
        //         is a better way
        EncryptKey pKey = facebook.getPublicKeyForEncryption(identifier);
        assert pKey != null : "failed to get encrypt key for user: " + identifier;
        return pKey.encrypt(plaintext, null);
    }

    //
    //  Interfaces for Local User
    //

    @Override
    public byte[] sign(byte[] data) {
        User.DataSource facebook = getDataSource();
        assert facebook != null : "user delegate not set yet";
        SignKey sKey = facebook.getPrivateKeyForSignature(identifier);
        assert sKey != null : "failed to get sign key for user: " + identifier;
        return sKey.sign(data);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        User.DataSource facebook = getDataSource();
        assert facebook != null : "user delegate not set yet";
        // NOTICE: if you provide a public key in visa for encryption,
        //         here you should return the private key paired with visa.key
        List<DecryptKey> keys = facebook.getPrivateKeysForDecryption(identifier);
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
        ID did = doc.getIdentifier();
        assert identifier.equals(did) : "visa ID not match: " + identifier + ", " + did;
        User.DataSource facebook = getDataSource();
        assert facebook != null : "user delegate not set yet";
        // NOTICE: only sign visa with the private key paired with your meta.key
        SignKey sKey = facebook.getPrivateKeyForVisaSignature(did);
        assert sKey != null : "failed to get sign key for visa: " + did;
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
        ID did = doc.getIdentifier();
        if (!identifier.equals(did)) {
            // visa ID not match
            return false;
        }
        VerifyKey pKey = getMeta().getPublicKey();
        assert pKey != null : "failed to get verify key for visa: " + did;
        return doc.verify(pKey);
    }

}
