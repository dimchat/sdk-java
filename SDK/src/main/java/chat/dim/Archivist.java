/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2023 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Albert Moky
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

import java.util.Date;
import java.util.List;

import chat.dim.mkm.Entity;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.utils.FrequencyChecker;
import chat.dim.utils.RecentTimeChecker;

public abstract class Archivist implements Entity.DataSource {

    // each query will be expired after 10 minutes
    public static final int QUERY_EXPIRES = 600 * 1000;  // milliseconds

    // query checkers
    private final FrequencyChecker<ID> metaQueries;
    private final FrequencyChecker<ID> docsQueries;
    private final FrequencyChecker<ID> membersQueries;

    // recent time checkers
    private final RecentTimeChecker<ID> lastDocumentTimes = new RecentTimeChecker<>();
    private final RecentTimeChecker<ID> lastHistoryTimes = new RecentTimeChecker<>();

    public Archivist(long lifeSpan) {
        super();
        assert lifeSpan > 0 : "query life span error: " + lifeSpan;
        metaQueries = new FrequencyChecker<>(lifeSpan);
        docsQueries = new FrequencyChecker<>(lifeSpan);
        membersQueries = new FrequencyChecker<>(lifeSpan);
    }

    protected boolean isMetaQueryExpired(ID identifier) {
        return metaQueries.isExpired(identifier, 0, false);
    }
    protected boolean isDocumentQueryExpired(ID identifier) {
        return docsQueries.isExpired(identifier, 0, false);
    }
    protected boolean isMembersQueryExpired(ID identifier) {
        return membersQueries.isExpired(identifier, 0, false);
    }

    /**
     *  check whether need to query meta
     */
    protected boolean needsQueryMeta(ID identifier, Meta meta) {
        if (identifier.isBroadcast()) {
            // broadcast entity has no meta to query
            return false;
        } else if (meta == null) {
            // meta not found, sure to query
            return true;
        }
        assert meta.matchIdentifier(identifier) : "meta not match: " + identifier + ", " + meta;
        return false;
    }

    //
    //  Last Document Times
    //

    public boolean setLastDocumentTime(ID identifier, Date current) {
        return lastDocumentTimes.setLastTime(identifier, current);
    }

    /**
     *  check whether need to query documents
     */
    protected boolean needsQueryDocuments(ID identifier, List<Document> documents) {
        if (identifier.isBroadcast()) {
            // broadcast entity has no document to query
            return false;
        } else if (documents == null || documents.isEmpty()) {
            // documents not found, sure to query
            return true;
        }
        Date current = getLastDocumentTime(identifier, documents);
        return lastDocumentTimes.isExpired(identifier, current);
    }
    protected Date getLastDocumentTime(ID identifier, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        Date lastTime = null;
        Date docTime;
        for (Document doc : documents) {
            assert doc.getIdentifier().equals(identifier) : "document not match: " + identifier + ", " + doc;
            docTime = doc.getTime();
            if (docTime == null) {
                assert false : "document error: " + doc;
            } else if (lastTime == null || lastTime.before(docTime)) {
                lastTime = docTime;
            }
        }
        return lastTime;
    }

    //
    //  Last Group History Times
    //

    public boolean setLastGroupHistoryTime(ID group, Date current) {
        return lastHistoryTimes.setLastTime(group, current);
    }

    /**
     *  check whether need to query group members
     */
    protected boolean needsQueryMembers(ID group, List<ID> members) {
        if (group.isBroadcast()) {
            // broadcast group has no members to query
            return false;
        } else if (members == null || members.isEmpty()) {
            // members not found, sure to query
            return true;
        }
        Date current = getLastGroupHistoryTime(group);
        return lastHistoryTimes.isExpired(group, current);
    }
    protected abstract Date getLastGroupHistoryTime(ID group);

    /**
     *  Check meta for querying
     *
     * @param identifier - entity ID
     * @param meta       - exists meta
     * @return ture on querying
     */
    public boolean checkMeta(ID identifier, Meta meta) {
        if (needsQueryMeta(identifier, meta)) {
            /*/
            if (!isMetaQueryExpired(identifier)) {
                // query not expired yet
                return false;
            }
            /*/
            return queryMeta(identifier);
        } else {
            // no need to query meta again
            return false;
        }
    }

    /**
     *  Check documents for querying/updating
     *
     * @param identifier - entity ID
     * @param documents  - exist document
     * @return true on querying
     */
    public boolean checkDocuments(ID identifier, List<Document> documents) {
        if (needsQueryDocuments(identifier, documents)) {
            /*/
            if (!isDocumentQueryExpired(identifier)) {
                // query not expired yet
                return false;
            }
            /*/
            return queryDocuments(identifier, documents);
        } else {
            // no need to update documents now
            return false;
        }
    }

    /**
     *  Check group members for querying
     *
     * @param group   - group ID
     * @param members - exist members
     * @return true on querying
     */
    public boolean checkMembers(ID group, List<ID> members) {
        if (needsQueryMembers(group, members)) {
            /*/
            if (!isMembersQueryExpired(group)) {
                // query not expired yet
                return false;
            }
            /*/
            return queryMembers(group, members);
        } else {
            // no need to update group members now
            return false;
        }
    }

    /**
     *  Request for meta with entity ID
     *  (call 'isMetaQueryExpired()' before sending command)
     *
     * @param identifier - entity ID
     * @return false on duplicated
     */
    public abstract boolean queryMeta(ID identifier);

    /**
     *  Request for documents with entity ID
     *  (call 'isDocumentQueryExpired()' before sending command)
     *
     * @param identifier - entity ID
     * @param documents  - exist documents
     * @return false on duplicated
     */
    public abstract boolean queryDocuments(ID identifier, List<Document> documents);

    /**
     *  Request for group members with group ID
     *  (call 'isMembersQueryExpired()' before sending command)
     *
     * @param group      - group ID
     * @param members    - exist members
     * @return false on duplicated
     */
    public abstract boolean queryMembers(ID group, List<ID> members);

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

}
