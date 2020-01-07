/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
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
package chat.dim.protocol;

import java.nio.charset.Charset;
import java.util.Map;

import chat.dim.ID;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.Base64;
import chat.dim.format.JSON;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "storage",
 *      title   : "key name",  // "contacts", "private_key", ...
 *
 *      data    : "...",       // base64_encode(symmetric)
 *      key     : "...",       // base64_encode(asymmetric)
 *
 *      // -- extra info
 *      //...
 *  }
 */
public class StorageCommand extends Command {

    public static final String STORAGE = "storage";

    // storage titles (should be encrypted)
    public static final String CONTACTS = "contacts";
    public static final String PRIVATE_KEY = "private_key";
    //...

    public final String title;

    private byte[] data = null;
    private byte[] key = null;

    private byte[] plaintext = null;
    private SymmetricKey password = null;

    public StorageCommand(Map<String, Object> dictionary) {
        super(dictionary);
        Object value = dictionary.get("title");
        if (value == null) {
            // (compatible with v1.0)
            //  contacts command: {
            //      command : 'contacts',
            //      data    : '...',
            //      key     : '...'
            //  }
            title = (String) dictionary.get("command");
            assert !title.equalsIgnoreCase(STORAGE) : "title error: " + title;
        } else {
            title = (String) value;
        }
    }

    public StorageCommand(String name) {
        super(STORAGE);
        title = name;
    }

    public String getIdentifier() {
        return (String) dictionary.get("ID");
    }

    public void setIdentifier(ID identifier) {
        if (identifier == null) {
            dictionary.remove("ID");
        } else {
            dictionary.put("ID", identifier);
        }
    }

    //
    //  Encrypted data
    //      encrypted by a random password before upload
    //
    public byte[] getData() {
        if (data == null) {
            String base64 = (String) dictionary.get("data");
            if (base64 != null) {
                data = Base64.decode(base64);
            }
        }
        return data;
    }

    public void setData(byte[] value) {
        if (value == null) {
            dictionary.remove("data");
        } else {
            dictionary.put("data", Base64.encode(value));
        }
        data = value;
        plaintext = null;
    }

    //
    //  Symmetric key
    //      password to decrypt data
    //      encrypted by user's public key before upload.
    //      this should be empty when the storage data is "private_key".
    //
    public byte[] getKey() {
        if (key == null) {
            String base64 = (String) dictionary.get("key");
            if (base64 != null) {
                key = Base64.decode(base64);
            }
        }
        return key;
    }

    public void setKey(byte[] value) {
        if (value == null) {
            dictionary.remove("key");
        } else {
            dictionary.put("key", Base64.encode(value));
        }
        key = value;
        password = null;
    }

    //-------- Decryption

    public byte[] decrypt(SymmetricKey key) {
        if (plaintext == null) {
            assert key != null : "password should not be empty";
            byte[] data = getData();
            assert data != null : "encrypted data not found: " + dictionary;
            plaintext = key.decrypt(data);
        }
        return plaintext;
    }

    public byte[] decrypt(PrivateKey privateKey) throws ClassNotFoundException {
        if (password == null) {
            assert privateKey instanceof DecryptKey : "private key error: " + privateKey;
            byte[] key = getKey();
            assert key != null : "key data not found: " + dictionary;
            key = ((DecryptKey) privateKey).decrypt(key);
            assert key != null : "failed to decrypt key with: " + privateKey;
            String json = new String(key, Charset.forName("UTF-8"));
            password = SymmetricKey.getInstance(JSON.decode(json));
        }
        return decrypt(password);
    }
}
