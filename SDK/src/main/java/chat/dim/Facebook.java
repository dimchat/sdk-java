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

import chat.dim.core.Archivist;
import chat.dim.core.Barrack;
import chat.dim.mkm.Entity;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.EncryptKey;
import chat.dim.protocol.ID;
import chat.dim.protocol.VerifyKey;

public abstract class Facebook implements Entity.Delegate, User.DataSource, Group.DataSource {

    protected abstract Barrack getBarrack();

    public abstract Archivist getArchivist();

    /**
     *  Select local user for receiver
     *
     * @param receiver - user/group ID
     * @return local user
     */
    public ID selectLocalUser(ID receiver) {
        Archivist archivist = getArchivist();
        assert archivist != null : "archivist not ready";
        List<ID> allUsers = archivist.getLocalUsers();
        //
        //  1.
        //
        if (allUsers == null || allUsers.isEmpty()) {
            assert false : "local users should not be empty";
            return null;
        } else if (receiver.isBroadcast()) {
            // broadcast message can be decrypted by anyone, so
            // just return current user here
            return allUsers.get(0);
        }
        //
        //  2.
        //
        if (receiver.isUser()) {
            // personal message
            for (ID item : allUsers) {
                if (receiver.equals(item)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        } else if (receiver.isGroup()) {
            // group message (recipient not designated)
            //
            // the messenger will check group info before decrypting message,
            // so we can trust that the group's meta & members MUST exist here.
            List<ID> members = getMembers(receiver);
            if (members == null || members.isEmpty()) {
                assert false : "members not found: " + receiver;
                return null;
            }
            for (ID item : allUsers) {
                if (members.contains(item)) {
                    // DISCUSS: set this item to be current user?
                    return item;
                }
            }
        } else {
            assert false : "receiver error: " + receiver;
        }
        // not me?
        return null;
    }

    //-------- Entity Delegate

    @Override
    public User getUser(ID uid) {
        assert uid.isUser() : "user ID error: " + uid;
        Barrack barrack = getBarrack();
        assert barrack != null : "barrack not ready";
        //
        //  1. get from user cache
        //
        User user = barrack.getUser(uid);
        if (user != null) {
            return user;
        }
        //
        //  2. check visa key
        //
        if (!uid.isBroadcast()) {
            EncryptKey visaKey = getPublicKeyForEncryption(uid);
            if (visaKey == null) {
                assert false : "visa.key not found: " + uid;
                return null;
            }
            // NOTICE: if visa.key exists, then visa & meta must exist too.
        }
        //
        //  3. create user and cache it
        //
        user = barrack.createUser(uid);
        if (user != null) {
            barrack.cacheUser(user);
        }
        return user;
    }

    @Override
    public Group getGroup(ID gid) {
        assert gid.isGroup() : "group ID error: " + gid;
        Barrack barrack = getBarrack();
        assert barrack != null : "barrack not ready";
        //
        //  1. get from group cache
        //
        Group group = barrack.getGroup(gid);
        if (group != null) {
            return group;
        }
        //
        //  2. check members
        //
        if (!gid.isBroadcast()) {
            List<ID> members = getMembers(gid);
            if (members == null || members.isEmpty()) {
                assert false : "group members not found: " + gid;
                return null;
            }
            // NOTICE: if members exist, then owner (founder) must exist,
            //         and bulletin & meta must exist too.
        }
        //
        //  3. create group and cache it
        //
        group = barrack.createGroup(gid);
        if (group != null) {
            barrack.cacheGroup(group);
        }
        return group;
    }

    //-------- User DataSource

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        assert user.isUser() : "user ID error: " + user;
        Archivist archivist = getArchivist();
        assert archivist != null : "archivist not ready";
        //
        //  1. get pubic key from visa
        //
        EncryptKey visaKey = archivist.getVisaKey(user);
        if (visaKey != null) {
            // if visa.key exists, use it for encryption
            return visaKey;
        }
        //
        //  2. get key from meta
        //
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
        assert archivist != null : "archivist not ready";
        //
        //  1. get pubic key from visa
        //
        EncryptKey visaKey = archivist.getVisaKey(user);
        if (visaKey instanceof VerifyKey) {
            // the sender may use communication key to sign message.data,
            // try to verify it with visa.key first
            keys.add((VerifyKey) visaKey);
        }
        //
        //  2. get key from meta
        //
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
