/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
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
 *  Entity pool for managing and caching User/Group instances (account entity manager).
 *  <p>
 *      Core responsibilities:
 *      1. In-memory caching of User/Group entities to avoid repeated creation
 *      2. Lazy creation of User/Group entities when required metadata is available
 *      3. Fast lookup of entities by ID (identifier)
 *  </p>
 *  <p>
 *      Key design: Acts as a "barracks" (entity pool) to centralize entity management,
 *      ensuring only one instance exists per ID and reducing redundant data loading.
 *  </p>
 */
public interface Barrack {

    /**
     *  Caches a User entity in memory (overwrites existing entry for the same ID).
     *
     * @param user is the user entity to cache (must have a valid ID).
     */
    void cacheUser(User user);

    /**
     *  Caches a Group entity in memory (overwrites existing entry for the same ID).
     *
     * @param group is the group entity to cache (must have a valid ID).
     */
    void cacheGroup(Group group);

    /**
     *  Retrieves a cached User entity by ID.
     *
     * @param uid is the unique ID of the target user.
     * @return a cached User instance (null if not found in cache).
     */
    User getUser(ID uid);

    /**
     *  Retrieves a cached Group entity by ID.
     *
     * @param gid is the unique ID of the target group.
     * @return a cached Group instance (null if not found in cache).
     */
    Group getGroup(ID gid);

    /**
     *  Creates a User entity if the required visa key metadata exists.
     *  <p>
     *      Lazy creation rule: Only creates a User when the user's visa.key (public key)
     *      is available (entity is "ready" for use). Does not cache the created user automatically.
     *  </p>
     *
     * @param uid is the unique ID of the user to create.
     * @return a new User instance (null if visa.key is missing/entity not ready).
     */
    User createUser(ID uid);

    /**
     *  Creates a Group entity if the required member list exists.
     *  <p>
     *      Lazy creation rule: Only creates a Group when the group's member list is available
     *      (entity is "ready" for use). Does not cache the created group automatically.
     *  </p>
     *
     * @param gid is the unique ID of the group to create.
     * @return a new Group instance (null if members are missing/entity not ready).
     */
    Group createGroup(ID gid);

    //
    //  Archivist
    //

    // -------------------------------------------------------------------------
    //  Local User Management (Critical for Message Decryption)
    // -------------------------------------------------------------------------

    /**
     *  Retrieves all local user IDs (used for decrypting received messages).
     *  <p>
     *      Local users are accounts logged into the current device with private keys,
     *      required to decrypt incoming personal/group messages targeted to the device.
     *  </p>
     *
     * @return the list of local user IDs (non-empty in normal operation).
     */
    List<ID> getLocalUsers();

}
