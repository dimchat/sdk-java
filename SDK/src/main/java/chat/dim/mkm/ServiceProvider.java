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
import java.util.Map;

import chat.dim.protocol.Document;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;

/**
 *  DIM Station Owner
 */
public class ServiceProvider extends BaseGroup {

    public ServiceProvider(ID identifier) {
        super(identifier);
        assert EntityType.ISP.equals(identifier.getType()) : "SP ID error: " + identifier;
    }

    /**
     *  Provider Document
     */
    public Document getProfile() {
        List<Document> documents = getDocuments();
        return DocumentUtils.lastDocument(documents, "*");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getStations() {
        Document doc = getProfile();
        if (doc != null) {
            Object stations = doc.getProperty("stations");
            if (stations instanceof List) {
                return (List<Map<String, Object>>) stations;
            }
        }
        // TODO: load from local storage
        return null;
    }

}
