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
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;


// -----------------------------------------------------------------------------
//  Core Entity Interfaces (User/Group Base)
// -----------------------------------------------------------------------------

/**
 * Base interface for all network entities (User/Group).
 *
 * Defines the core properties and data access patterns for entities in the communication system.
 * Entities are identified by a unique ID and have associated metadata (Meta) and extended documents
 * (e.g., Visa for Users, Bulletin for Groups).
 *
 * Core properties:
 * - identifier : Unique ID of the entity (user/group ID)
 * - type       : Numeric type identifier for the entity (user = 0, group = 1, etc.)
 * - meta       : Cryptographic metadata used to generate the entity ID
 * - documents  : Extended information (Visa for users, Bulletin for groups)
 */
public interface Entity {

    /**
     * Unique identifier of the entity (user/group ID).
     *
     * Serves as the primary key for identifying the entity in the system.
     *
     * @return entity ID
     */
    ID getIdentifier();

    /**
     * Numeric type identifier of the entity (EntityType).
     *
     * Common values:
     * - 0 : User entity
     * - 1 : Group entity
     * - ...
     *
     * @return network type
     */
    int getType();

    /**
     * Data source delegate for retrieving entity data (Meta/Documents).
     *
     * If set, the entity will use this delegate to fetch metadata and documents instead of
     * internal implementation, enabling flexible data sourcing (local/remote).
     *
     * @param dataSource data source delegate
     */
    void setDataSource(DataSource dataSource);

    /**
     * Data source delegate for retrieving entity data (Meta/Documents).
     *
     * If set, the entity will use this delegate to fetch metadata and documents instead of
     * internal implementation, enabling flexible data sourcing (local/remote).
     *
     * @return data source delegate
     */
    DataSource getDataSource();

    /**
     * Cryptographic metadata of the entity.
     *
     * Contains the core public key and type information used to generate the entity's ID.
     * Fetched from {@code dataSource} if available, otherwise from internal storage.
     *
     * @return the entity's core Meta object
     */
    Meta getMeta();

    /**
     * Extended documents associated with the entity.
     *
     * - For users: Contains Visa documents (identity/authorization info with terminal data)
     * - For groups: Contains Bulletin documents (group info/announcements)
     *
     * @return the list of entity documents (empty list if none)
     */
    List<Document> getDocuments();

    /**
     * Data source interface for retrieving entity metadata and documents.
     *
     * Defines the contract for fetching core entity data, enabling separation of data storage
     * (local database, remote API) from entity logic.
     *
     * Key data responsibilities:
     * 1. User Meta: Generated from the user's private key (contains public key for verification)
     * 2. Group Meta: Generated from the group founder's private key
     * 3. Meta Public Key: Used to verify messages sent by the user/group founder
     * 4. Visa Public Key: Used to encrypt messages for the user (terminal-specific)
     */
    interface DataSource {

        /**
         * Retrieves the metadata for a specific entity.
         *
         * @param did is the unique ID of the target entity (user/group)
         * @return the meta object for the entity (null if not found)
         */
        Meta getMeta(ID did);

        /**
         * Retrieves the extended documents for a specific entity.
         *
         * @param did is the unique ID of the target entity (user/group)
         * @return the list of documents (Visa/Bulletin) associated with the entity (empty list if none)
         */
        List<Document> getDocuments(ID did);
    }

    /**
     * Delegate interface for creating User/Group instances.
     *
     * Provides a factory pattern for entity instantiation, enabling centralized management
     * of user/group creation logic (e.g., caching, dependency injection).
     */
    interface Delegate {

        /**
         * Creates/retrieves a User instance for a specific ID.
         *
         * @param uid is the unique ID of the target user
         * @return the user instance (null if the user does not exist)
         */
        User getUser(ID uid);

        /**
         * Creates/retrieves a Group instance for a specific ID.
         *
         * @param gid is the unique ID of the target group
         * @return the group instance (null if the group does not exist)
         */
        Group getGroup(ID gid);
    }
}
