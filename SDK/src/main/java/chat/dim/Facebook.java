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

import java.util.List;

import chat.dim.mkm.BaseGroup;
import chat.dim.mkm.BaseUser;
import chat.dim.mkm.Bot;
import chat.dim.mkm.DocumentHelper;
import chat.dim.mkm.Group;
import chat.dim.mkm.ServiceProvider;
import chat.dim.mkm.Station;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public abstract class Facebook extends Barrack {

    protected abstract Archivist getArchivist();

    @Override
    protected User createUser(ID identifier) {
        assert identifier.isUser() : "user ID error: " + identifier;
        // check visa key
        if (!identifier.isBroadcast()) {
            if (getPublicKeyForEncryption(identifier) == null) {
                assert false : "visa.key not found: " + identifier;
                return null;
            }
            // NOTICE: if visa.key exists, then visa & meta must exist too.
        }
        int type = identifier.getType();
        // check user type
        if (EntityType.STATION.equals(type)) {
            return new Station(identifier);
        } else if (EntityType.BOT.equals(type)) {
            return new Bot(identifier);
        }
        // general user, or 'anyone@anywhere'
        return new BaseUser(identifier);
    }

    @Override
    protected Group createGroup(ID identifier) {
        assert identifier.isGroup() : "group ID error: " + identifier;
        // check members
        if (!identifier.isBroadcast()) {
            List<ID> members = getMembers(identifier);
            if (members == null || members.isEmpty()) {
                assert false : "group members not found: " + identifier;
                return null;
            }
            // NOTICE: if members exist, then owner (founder) must exist,
            //         and bulletin & meta must exist too.
        }
        int type = identifier.getType();
        // check group type
        if (EntityType.ISP.equals(type)) {
            return new ServiceProvider(identifier);
        }
        // general group, or 'everyone@everywhere'
        return new BaseGroup(identifier);
    }

    /**
     *  Get all local users (for decrypting received message)
     *
     * @return users with private key
     */
    public abstract List<User> getLocalUsers();

    /**
     *  Select local user for receiver
     *
     * @param receiver - user/group ID
     * @return local user
     */
    public User selectLocalUser(ID receiver) {
        List<User> users = getLocalUsers();
        if (users == null || users.isEmpty()) {
            assert false : "local users should not be empty";
            return null;
        } else if (receiver.isBroadcast()) {
            // broadcast message can decrypt by anyone, so just return current user
            return users.get(0);
        } else if (receiver.isUser()) {
            // 1. personal message
            // 2. split group message
            for (User item : users) {
                if (receiver.equals(item.getIdentifier())) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
            // not me?
            return null;
        }
        // group message (recipient not designated)
        assert receiver.isGroup() : "receiver error: " + receiver;
        // the messenger will check group info before decrypting message,
        // so we can trust that the group's meta & members MUST exist here.
        List<ID> members = getMembers(receiver);
        assert !members.isEmpty() : "members not found: " + receiver;
        for (User item : users) {
            if (members.contains(item.getIdentifier())) {
                // DISCUSS: set this item to be current user?
                return item;
            }
        }
        return null;
    }

    public boolean saveMeta(Meta meta, ID identifier) {
        boolean ok = meta.isValid() && meta.matchIdentifier(identifier);
        if (!ok) {
            assert false : "meta not valid: " + identifier;
            return false;
        }
        // check old meta
        Meta old = getMeta(identifier);
        if (old != null) {
            assert meta.equals(old) : "meta would not changed";
            return true;
        }
        // meta not exists yet, save it
        Archivist archivist = getArchivist();
        return archivist.saveMeta(meta, identifier);
    }

    public boolean saveDocument(Document doc) {
        ID identifier = doc.getIdentifier();
        if (identifier == null) {
            assert false : "document error: " + doc;
            return false;
        }
        if (!doc.isValid()) {
            // try to verify
            Meta meta = getMeta(identifier);
            if (meta == null) {
                assert false : "meta not found: " + identifier;
                return false;
            } else if (doc.verify(meta.getPublicKey())) {
                assert false : "document verified: " + identifier;
            } else {
                assert false : "failed to verify document: " + identifier;
                return false;
            }
        }
        String type = doc.getType();
        // check old documents with type
        List<Document> documents = getDocuments(identifier);
        Document old = DocumentHelper.lastDocument(documents, type);
        if (old != null && DocumentHelper.isExpired(doc, old)) {
            // assert false : "drop expired document: " + identifier;
            return false;
        }
        Archivist archivist = getArchivist();
        return archivist.saveDocument(doc);
    }

    //
    //  EntityDataSource
    //

    @Override
    public Meta getMeta(ID entity) {
        /*/
        if (entity.isBroadcast()) {
            // broadcast ID has no meta
            return null;
        }
        /*/
        Archivist archivist = getArchivist();
        Meta meta = archivist.getMeta(entity);
        archivist.checkMeta(entity, meta);
        return meta;
    }

    @Override
    public List<Document> getDocuments(ID entity) {
        /*/
        if (entity.isBroadcast()) {
            // broadcast ID has no documents
            return null;
        }
        /*/
        Archivist archivist = getArchivist();
        List<Document> docs = archivist.getDocuments(entity);
        archivist.checkDocuments(entity, docs);
        return docs;
    }

}
