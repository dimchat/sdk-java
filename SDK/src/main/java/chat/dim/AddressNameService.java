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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.Address;
import chat.dim.protocol.ID;

public abstract class AddressNameService {

    public static final ID FOUNDER = ID.create("moky", Address.ANYWHERE, null);
    public static final String[] KEYWORDS = {
            "all", "everyone", "anyone", "owner", "founder",
            // --------------------------------
            "dkd", "mkm", "dimp", "dim", "dimt",
            "rsa", "ecc", "aes", "des", "btc", "eth",
            // --------------------------------
            "crypto", "key", "symmetric", "asymmetric",
            "public", "private", "secret", "password",
            "id", "address", "meta", "profile", "document",
            "entity", "user", "group", "contact",
            // --------------------------------
            "member", "admin", "administrator", "assistant",
            "main", "polylogue", "chatroom",
            "social", "organization",
            "company", "school", "government", "department",
            "provider", "station", "thing", "robot",
            // --------------------------------
            "message", "instant", "secure", "reliable",
            "envelope", "sender", "receiver", "time",
            "content", "forward", "command", "history",
            "keys", "data", "signature",
            // --------------------------------
            "type", "serial", "sn",
            "text", "file", "image", "audio", "video", "page",
            "handshake", "receipt", "block", "mute",
            "register", "suicide", "found", "abdicate",
            "invite", "expel", "join", "quit", "reset", "query",
            "hire", "fire", "resign",
            // --------------------------------
            "server", "client", "terminal", "local", "remote",
            "barrack", "cache", "transceiver",
            "ans", "facebook", "store", "messenger",
            "root", "supervisor",
    };

    private final Map<String, Boolean> reserved = new HashMap<>();
    private final Map<String, ID> caches = new HashMap<>();

    protected AddressNameService() {
        super();
        // constant ANS records
        caches.put("all", ID.EVERYONE);
        caches.put("everyone", ID.EVERYONE);
        caches.put("anyone", ID.ANYONE);
        caches.put("owner", ID.ANYONE);
        caches.put("founder", FOUNDER);
        // reserved names
        for (String item : KEYWORDS) {
            reserved.put(item, true);
        }
    }

    protected boolean isReserved(String name) {
        Boolean value = reserved.get(name);
        if (value == null) {
            return false;
        }
        return value;
    }

    protected boolean cache(String name, ID identifier) {
        if (isReserved(name)) {
            // this name is reserved, cannot register
            return false;
        }
        if (identifier == null) {
            caches.remove(name);
        } else {
            caches.put(name, identifier);
        }
        return true;
    }

    /**
     *  Get ID by short name
     *
     * @param name - sort name
     * @return user ID
     */
    public ID identifier(String name) {
        return caches.get(name);
    }

    /**
     *  Get all short names with the same ID
     *
     * @param identifier - user ID
     * @return short name list
     */
    public List<String> names(ID identifier) {
        List<String> array = new ArrayList<>();
        for (Map.Entry<String, ID> entry : caches.entrySet()) {
            if (identifier.equals(entry.getValue())) {
                array.add(entry.getKey());
            }
        }
        return array;
    }

    /**
     *  Save ANS record
     *
     * @param name - username
     * @param identifier - user ID; if empty, means delete this name
     * @return true on success
     */
    public boolean save(String name, ID identifier) {
        return cache(name, identifier);
    }
}
