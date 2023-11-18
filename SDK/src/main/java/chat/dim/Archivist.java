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

import chat.dim.mkm.DocumentHelper;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.utils.FrequencyChecker;
import chat.dim.utils.RecentTimeChecker;

public abstract class Archivist {

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
    protected boolean needsUpdateDocuments(ID identifier, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            // documents not found, sure to update
            return true;
        }
        Date current = getLastDocumentTime(identifier, documents);
        return lastDocumentTimes.isExpired(identifier, current);
    }
    protected Date getLastDocumentTime(ID identifier, List<Document> documents) {
        Document doc = DocumentHelper.lastDocument(documents, null);
        if (doc == null) {
            return null;
        } else {
            return doc.getTime();
        }
    }

    //
    //  Last Group History Times
    //

    public boolean setLastGroupHistoryTime(ID group, Date current) {
        return lastHistoryTimes.setLastTime(group, current);
    }
    protected boolean needsUpdateGroupHistory(ID group, List<ID> members) {
        if (members == null || members.isEmpty()) {
            // members not found, sure to update
            return true;
        }
        Date current = getLastGroupHistoryTime(group, members);
        return lastHistoryTimes.isExpired(group, current);
    }
    protected abstract Date getLastGroupHistoryTime(ID group, List<ID> members);

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
        Date lastTime = getLastDocumentTime(identifier, documents);
        return queryDocuments(identifier, lastTime);
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
        Date lastTime = getLastGroupHistoryTime(group, members);
        return queryMembers(group, lastTime);
    }

    /**
     *  Request for meta with entity ID
     *  (call 'isMetaQueryExpired()' before sending command)
     *
     * @param identifier - entity ID
     * @return false on duplicated
     */
    protected abstract boolean queryMeta(ID identifier);

    /**
     *  Request for documents with entity ID
     *  (call 'isDocumentQueryExpired()' before sending command)
     *
     * @param identifier - entity ID
     * @param lastTime   - last document time
     * @return false on duplicated
     */
    protected abstract boolean queryDocuments(ID identifier, Date lastTime);

    /**
     *  Request for group members with group ID
     *  (call 'isMembersQueryExpired()' before sending command)
     *
     * @param group      - group ID
     * @param lastTime   - last group history time
     * @return false on duplicated
     */
    protected abstract boolean queryMembers(ID group, Date lastTime);

}
