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

import chat.dim.core.Barrack;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.mkm.Entity;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public abstract class Facebook implements Entity.Delegate, User.DataSource, Group.DataSource {

    protected abstract Barrack getBarrack();

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

    //
    //  Public Keys
    //

    /**
     *  Get meta.key
     *
     * @param user - user ID
     * @return null on not found
     */
    protected abstract VerifyKey getMetaKey(ID user);

    /**
     *  Get visa.key
     *
     * @param user - user ID
     * @return null on not found
     */
    protected abstract EncryptKey getVisaKey(ID user);

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
        Barrack barrack = getBarrack();
        List<User> users = barrack.getLocalUsers();
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

    //-------- Entity Delegate

    @Override
    public User getUser(ID identifier) {
        assert identifier.isUser() : "user ID error: " + identifier;
        Barrack barrack = getBarrack();
        //
        //  1. get from user cache
        //
        User user = barrack.getUser(identifier);
        if (user != null) {
            return user;
        }
        //
        //  2. check visa key
        //
        if (!identifier.isBroadcast()) {
            if (getPublicKeyForEncryption(identifier) == null) {
                assert false : "visa.key not found: " + identifier;
                return null;
            }
            // NOTICE: if visa.key exists, then visa & meta must exist too.
        }
        //
        //  3. create user and cache it
        //
        user = barrack.createUser(identifier);
        if (user != null) {
            user.setDataSource(this);
            barrack.cacheUser(user);
        }
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        assert identifier.isGroup() : "group ID error: " + identifier;
        Barrack barrack = getBarrack();
        //
        //  1. get from group cache
        //
        Group group = barrack.getGroup(identifier);
        if (group != null) {
            return group;
        }
        //
        //  2. check members
        //
        if (!identifier.isBroadcast()) {
            List<ID> members = getMembers(identifier);
            if (members == null || members.isEmpty()) {
                assert false : "group members not found: " + identifier;
                return null;
            }
            // NOTICE: if members exist, then owner (founder) must exist,
            //         and bulletin & meta must exist too.
        }
        //
        //  3. create group and cache it
        //
        group = barrack.createGroup(identifier);
        if (group != null) {
            group.setDataSource(this);
            barrack.cacheGroup(group);
        }
        return group;
    }

    //-------- User DataSource

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        assert user.isUser() : "user ID error: " + user;
        // 1. get pubic key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey != null) {
            // if visa.key exists, use it for encryption
            return visaKey;
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
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
        // 1. get pubic key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey instanceof VerifyKey) {
            // the sender may use communication key to sign message.data,
            // try to verify it with visa.key first
            keys.add((VerifyKey) visaKey);
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey != null) {
            // the sender may use identity key to sign message.data,
            // try to verify it with meta.key too
            keys.add(metaKey);
        }
        assert !keys.isEmpty() : "failed to get verify key for user: " + user;
        return keys;
    }

}
