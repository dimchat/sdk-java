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
import chat.dim.protocol.NetworkType;

/**
 *  Big group with admins
 */
public class Chatroom extends BaseGroup {

    public Chatroom(ID identifier) {
        super(identifier);
        assert NetworkType.CHATROOM.equals(identifier.getType()) : "chatroom ID error: " + identifier;
    }

    @Override
    public DataSource getDataSource() {
        return (DataSource) super.getDataSource();
    }

    public List<ID> getAdmins() {
        DataSource dataSource = getDataSource();
        return dataSource.getAdmins(identifier);
    }

    /**
     *  This interface is for getting information for chatroom
     *  Chatroom admins should be set complying with the consensus algorithm
     */
    public interface DataSource extends Group.DataSource {

        /**
         *  Get all admins in the chatroom
         *
         * @param chatroom - chatroom ID
         * @return admin ID list
         */
        List<ID> getAdmins(ID chatroom);
    }
}
