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
package chat.dim.mkm.plugins;

import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PublicKey;

public class UserProfile extends Profile {

    public UserProfile(Map<String, Object> dictionary) {
        super(dictionary);
        Object identifier = dictionary.get("ID");
        if (identifier instanceof ID) {
            if (!((ID) identifier).getType().isUser()) {
                throw new ClassCastException("not a user profile: " + dictionary);
            }
        } else if (!dictionary.containsKey("avatar") &&
                !dictionary.containsKey("key")) {
            throw new ClassCastException("not a user profile: " + dictionary);
        }
    }

    public UserProfile(ID identifier) {
        super(identifier);
        assert identifier.getType().isUser() : "user ID error: " + identifier;
    }

    /**
     *  Public key (used for encryption, can be same with meta.key)
     *
     *      RSA
     */
    private EncryptKey key = null;

    @Override
    public EncryptKey getKey() {
        if (key == null) {
            // get public key
            try {
                key = (EncryptKey) PublicKey.getInstance(getProperty("key"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return key;
    }

    @Override
    public void setKey(EncryptKey publicKey) {
        key = publicKey;
        setProperty("key", key);
    }

    /**
     *  Get user name
     *
     * @return nickname
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        String name = super.getName();
        if (name == null) {
            // get from 'names'
            Object array = getProperty("names");
            if (array != null) {
                List<String> names = (List<String>) array;
                if (names.size() > 0) {
                    name = names.get(0);
                }
            }
        }
        return name;
    }

    @SuppressWarnings("unchecked")
    public String getAvatar() {
        String url = (String) getProperty("avatar");
        if (url == null) {
            // get from 'photos'
            Object array = getProperty("photos");
            if (array != null) {
                List<String> photos = (List<String>) array;
                if (photos.size() > 0) {
                    url = photos.get(0);
                }
            }
        }
        return url;
    }

    public void setAvatar(String url) {
        setProperty("avatar", url);
    }
}
