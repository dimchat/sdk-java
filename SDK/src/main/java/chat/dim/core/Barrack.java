/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim.core;

import java.util.List;

import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.ID;

/**
 *  Entity Factory
 *  ~~~~~~~~~~~~~~
 *  Entity pool to manage User/Group instances
 */
public interface Barrack {

    void cacheUser(User user);

    void cacheGroup(Group group);

    //
    //  Entity Delegate
    //

    User getUser(ID identifier);

    Group getGroup(ID identifier);

    //
    //  Archivist
    //

    /**
     *  Create user when visa.key exists
     *
     * @param identifier - user ID
     * @return user, null on not ready
     */
    User createUser(ID identifier);

    /**
     *  Create group when members exist
     *
     * @param identifier - group ID
     * @return group, null on not ready
     */
    Group createGroup(ID identifier);

    /**
     *  Get all local users (for decrypting received message)
     *
     * @return users with private key
     */
    List<User> getLocalUsers();

}
