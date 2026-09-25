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

import chat.dim.protocol.ID;


// -----------------------------------------------------------------------------
//  Group Entity
// -----------------------------------------------------------------------------

/**
 * Group entity interface representing a chat group.
 *
 * Extends {@link Entity} with group-specific properties and role management.
 *
 * Groups have a hierarchical role structure:
 * - Founder        : Original creator of the group (immutable)
 * - Owner          : Current administrator of the group (can be transferred)
 * - Members        : Regular participants in the group
 * - Administrators : Optional role for privileged members (assistants)
 *
 * Important note: The group owner must always be a member of the group (usually the first member).
 */
public interface Group extends Entity {

    /**
     * Founder ID of the group.
     *
     * The original creator of the group (cannot be changed after group creation).
     * The founder's private key is used to generate the group's Meta.
     *
     * @return the group founder's ID
     */
    ID getFounder();

    /**
     * Current owner ID of the group.
     *
     * The user with administrative control over the group (can be transferred via abdicate command).
     * Must be a member of the group.
     *
     * @return the current group owner's ID
     */
    ID getOwner();

    /**
     * List of all member IDs in the group.
     *
     * Includes the owner and all regular members (excludes founder if not a member).
     *
     * @return the list of group member IDs (empty list if none)
     */
    // NOTICE: the owner must be a member
    //         (usually the first one)
    List<ID> getMembers();

    /**
     * Data source interface for retrieving group-specific data.
     *
     * Extends {@link Entity.DataSource} with group role and membership management, defining
     * the contract for fetching group-specific data (founder, owner, members).
     *
     * Key rules:
     * 1. Founder's public key matches the group Meta's public key
     * 2. Owner/members must be managed according to the system's consensus algorithm
     */
    interface DataSource extends Entity.DataSource {

        /**
         * Retrieves the founder ID of a group.
         *
         * @param group is the unique ID of the target group
         * @return the founder ID (null if the group does not exist)
         */
        ID getFounder(ID group);

        /**
         * Retrieves the current owner ID of a group.
         *
         * @param group is the unique ID of the target group
         * @return the owner ID (null if the group does not exist or has no owner)
         */
        ID getOwner(ID group);

        /**
         * Retrieves the list of member IDs for a group.
         *
         * @param group is the unique ID of the target group
         * @return the list of member IDs (empty list if the group has no members)
         */
        List<ID> getMembers(ID group);

    }
}
