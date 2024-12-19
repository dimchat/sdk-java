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
import java.util.List;

import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public abstract class Facebook extends Barrack implements User.DataSource, Group.DataSource {

    public abstract Archivist getArchivist();

    @Override
    protected void cacheUser(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        super.cacheUser(user);
    }

    @Override
    protected void cacheGroup(Group group) {
        if (group.getDataSource() == null) {
            group.setDataSource(this);
        }
        super.cacheGroup(group);
    }

    //-------- Entity Delegate

    @Override
    public User getUser(ID identifier) {
        assert identifier.isUser() : "user ID error: " + identifier;
        // 1. get from user cache
        User user = super.getUser(identifier);
        if (user == null) {
            // 2. create user and cache it
            Archivist archivist = getArchivist();
            user = archivist.createUser(identifier);
            if (user != null) {
                cacheUser(user);
            }
        }
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        assert identifier.isGroup() : "group ID error: " + identifier;
        // 1. get from group cache
        Group group = super.getGroup(identifier);
        if (group == null) {
            // 2. create group and cache it
            Archivist archivist = getArchivist();
            group = archivist.createGroup(identifier);
            if (group != null) {
                cacheGroup(group);
            }
        }
        return group;
    }

    /**
     *  Select local user for receiver
     *
     * @param receiver - user/group ID
     * @return local user
     */
    public User selectLocalUser(ID receiver) {
        if (receiver.isGroup()) {
            // group message (recipient not designated)
            // TODO: check members of group
            return null;
        } else {
            assert receiver.isUser() : "receiver error: " + receiver;
        }
        Archivist archivist = getArchivist();
        List<User> users = archivist.getLocalUsers();
        if (users == null || users.isEmpty()) {
            assert false : "local users should not be empty";
            return null;
        } else if (receiver.isBroadcast()) {
            // broadcast message can be decrypted by anyone, so
            // just return current user here
            return users.get(0);
        }
        // 1. personal message
        // 2. split group message
        for (User item : users) {
            if (receiver.equals(item.getIdentifier())) {
                // DISCUSS: set this item to be current user?
                return item;
            }
        }
        // not mine?
        return null;
    }

    /**
     *  Save meta for entity ID (must verify first)
     *
     * @param meta - entity meta
     * @param identifier - entity ID
     * @return true on success
     */
    public abstract boolean saveMeta(Meta meta, ID identifier);

    /**
     *  Save entity document with ID (must verify first)
     *
     * @param doc - entity document
     * @return true on success
     */
    public abstract boolean saveDocument(Document doc);

    //-------- User DataSource

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        assert user.isUser() : "user ID error: " + user;
        Archivist archivist = getArchivist();
        // 1. get pubic key from visa
        EncryptKey visaKey = archivist.getVisaKey(user);
        if (visaKey != null) {
            // if visa.key exists, use it for encryption
            return visaKey;
        }
        // 2. get key from meta
        VerifyKey metaKey = archivist.getMetaKey(user);
        if (metaKey instanceof EncryptKey) {
            // if visa.key not exists and meta.key is encrypt key,
            // use it for encryption
            return (EncryptKey) metaKey;
        }
        //throw new NullPointerException("failed to get encrypt key for user: " + user);
        return null;
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        // assert user.isUser() : "user ID error: " + user;
        List<VerifyKey> keys = new ArrayList<>();
        Archivist archivist = getArchivist();
        // 1. get pubic key from visa
        EncryptKey visaKey = archivist.getVisaKey(user);
        if (visaKey instanceof VerifyKey) {
            // the sender may use communication key to sign message.data,
            // try to verify it with visa.key first
            keys.add((VerifyKey) visaKey);
        }
        // 2. get key from meta
        VerifyKey metaKey = archivist.getMetaKey(user);
        if (metaKey != null) {
            // the sender may use identity key to sign message.data,
            // try to verify it with meta.key too
            keys.add(metaKey);
        }
        assert !keys.isEmpty() : "failed to get verify key for user: " + user;
        return keys;
    }

}
