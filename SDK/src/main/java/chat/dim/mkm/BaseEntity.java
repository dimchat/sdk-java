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

import java.lang.ref.WeakReference;
import java.util.List;

import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class BaseEntity implements Entity {

    protected final ID identifier;

    private WeakReference<DataSource> facebookRef = null;

    public BaseEntity(ID did) {
        super();
        identifier = did;
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) {
            // same object
            return true;
        } else if (other instanceof Entity) {
            // check with ID
            Entity entity = (Entity) other;
            other = entity.getIdentifier();
        }
        return identifier.equals(other);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        String clazz = getClass().getSimpleName();
        int network = identifier.getAddress().getNetwork();
        return "<" + clazz + " id=\"" + identifier + "\" network=" + network + " />";
    }

    @Override
    public ID getIdentifier() {
        return identifier;
    }

    @Override
    public int getType() {
        return identifier.getType();
    }

    @Override
    public DataSource getDataSource() {
        return facebookRef == null ? null : facebookRef.get();
    }

    @Override
    public void setDataSource(DataSource facebook) {
        facebookRef = facebook == null ? null : new WeakReference<>(facebook);
    }

    @Override
    public Meta getMeta() {
        DataSource facebook = getDataSource();
        if (facebook == null) {
            assert false : "entity datasource not set yet";
            return null;
        }
        return facebook.getMeta(identifier);
    }

    @Override
    public List<Document> getDocuments() {
        DataSource facebook = getDataSource();
        if (facebook == null) {
            assert false : "entity datasource not set yet";
            return null;
        }
        return facebook.getDocuments(identifier);
    }

}
