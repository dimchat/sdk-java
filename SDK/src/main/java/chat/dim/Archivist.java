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

    //
    //  Last Document Times
    //

    public boolean setLastDocumentTime(ID identifier, Date current) {
        return lastDocumentTimes.setLastTime(identifier, current);
    }
    public boolean needsUpdateDocuments(ID identifier, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            // documents not found, sure to update
            return true;
        }
        Date current = getLastDocumentTime(identifier, documents);
        return lastDocumentTimes.isExpired(identifier, current);
    }
    public Date getLastDocumentTime(ID identifier, List<Document> documents) {
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
    public boolean needsUpdateGroupHistory(ID group, List<ID> members) {
        if (members == null || members.isEmpty()) {
            // members not found, sure to update
            return true;
        }
        Date current = getLastGroupHistoryTime(group);
        return lastHistoryTimes.isExpired(group, current);
    }
    public abstract Date getLastGroupHistoryTime(ID group);

    /**
     *  Check meta for querying
     *
     * @param identifier - entity ID
     * @param meta       - exists meta
     * @return ture on querying
     */
    public boolean checkMeta(ID identifier, Meta meta) {
        if (meta != null) {
            assert meta.matchIdentifier(identifier) : "meta not match: " + identifier;
            return false;
        }
        /*/
        if (!isMetaQueryExpired(identifier)) {
            // query not expired yet
            assert false : "meta query not expired yet: " + identifier;
            return false;
        }
        /*/
        return queryMeta(identifier);
    }

    /**
     *  Check documents for querying/updating
     *
     * @param identifier - entity ID
     * @param documents  - exist document
     * @return true on querying
     */
    public boolean checkDocuments(ID identifier, List<Document> documents) {
        if (!needsUpdateDocuments(identifier, documents)) {
            // no need to update documents now
            return false;
        }
        /*/
        if (!isDocumentQueryExpired(identifier)) {
            // query not expired yet
            assert false : "document query not expired yet: " + identifier;
            return false;
        }
        /*/
        return queryDocuments(identifier, documents);
    }

    /**
     *  Check group members for querying
     *
     * @param group   - group ID
     * @param members - exist members
     * @return true on querying
     */
    public boolean checkMembers(ID group, List<ID> members) {
        if (!needsUpdateGroupHistory(group, members)) {
            // no need to update group members now
            return false;
        }
        /*/
        if (!isMembersQueryExpired(group)) {
            // query not expired yet
            assert false : "members query not expired yet: " + group;
            return false;
        }
        /*/
        return queryMembers(group, members);
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
     * @param documents  - exixt documents
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
