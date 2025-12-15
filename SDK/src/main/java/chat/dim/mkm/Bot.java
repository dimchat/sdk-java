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
package chat.dim.mkm;

import java.util.List;

import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;

/**
 *  Bot User
 */
public class Bot extends BaseUser {

    protected List<Document> documents;

    public Bot(ID uid) {
        super(uid);
        assert EntityType.BOT.equals(uid.getType()) : "Bot ID error: " + uid;
        this.documents = null;
    }

    /**
     *  Reload bot info
     */
    public void reload() {
        documents = getDocuments();
    }

    /**
     *  Get last property
     */
    public Object getProfile(String key) {
        List<Document> docs = documents;
        if (docs == null) {
            return null;
        }
        // TODO: sort by doc.time DESC
        Object value;
        for (Document doc : docs) {
            value = doc.getProperty(key);
            if (value != null) {
                return value;
            }
        }
        // property not found
        return null;
    }

    /**
     *  Get provider ID
     *
     * @return ICP ID, bot group
     */
    public ID getProvider() {
        return ID.parse(getProfile("provider"));
    }

}
