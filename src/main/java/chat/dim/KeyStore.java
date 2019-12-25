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
package chat.dim;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import chat.dim.core.KeyCache;
import chat.dim.filesys.ExternalStorage;

public class KeyStore extends KeyCache {

    private User user = null;

    private static String separator = File.separator;

    public KeyStore() {
        super();
    }

    public User getUser() {
        return user;
    }
    public void setUser(User currentUser) {
        if (user != null) {
            // save key map for old user
            flush();
            if (user.equals(currentUser)) {
                // user not changed
                return;
            }
        }
        if (currentUser == null) {
            user = null;
            return;
        }
        // change current user
        user = currentUser;
        Map keys = loadKeys();
        if (keys == null) {
            // failed to load cached keys for new user
            return;
        }
        try {
            // update key map
            updateKeys(keys);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // '/tmp/.dim/protected/{ADDRESS}/keystore.js'
    private String getPath() {
        if (user == null) {
            return null;
        }
        return ExternalStorage.getPath() + separator + "protected" + separator
                + user.identifier.toString() + separator + "keystore.js";
    }

    @Override
    public boolean saveKeys(Map keyMap) {
        String path = getPath();
        if (path == null) {
            return false;
        }
        try {
            return ExternalStorage.saveJSON(keyMap, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map loadKeys() {
        String path = getPath();
        if (path == null) {
            return null;
        }
        try {
            return (Map) ExternalStorage.loadJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
