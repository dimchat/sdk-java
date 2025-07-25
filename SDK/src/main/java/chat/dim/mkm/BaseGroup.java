/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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

import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;

public class BaseGroup extends BaseEntity implements Group {

    // once the group founder is set, it will never change
    private ID founder = null;

    public BaseGroup(ID identifier) {
        super(identifier);
    }

    @Override
    public Group.DataSource getDataSource() {
        Entity.DataSource facebook = super.getDataSource();
        if (facebook instanceof Group.DataSource) {
            return (Group.DataSource) facebook;
        } else {
            assert facebook == null : "group data source error: " + facebook;
            return null;
        }
    }

    @Override
    public Bulletin getBulletin() {
        List<Document> documents = getDocuments();
        return DocumentUtils.lastBulletin(documents);
    }

    @Override
    public ID getFounder() {
        if (founder == null) {
            Group.DataSource facebook = getDataSource();
            assert facebook != null : "group delegate not set yet";
            founder = facebook.getFounder(identifier);
        }
        return founder;
    }

    @Override
    public ID getOwner() {
        Group.DataSource facebook = getDataSource();
        assert facebook != null : "group delegate not set yet";
        return facebook.getOwner(identifier);
    }

    @Override
    public List<ID> getMembers() {
        Group.DataSource facebook = getDataSource();
        assert facebook != null : "group delegate not set yet";
        return facebook.getMembers(identifier);
    }

    @Override
    public List<ID> getAssistants() {
        Group.DataSource facebook = getDataSource();
        assert facebook != null : "group delegate not set yet";
        return facebook.getAssistants(identifier);
    }

}
