/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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

import java.util.Map;

import chat.dim.format.TransportableData;
import chat.dim.plugins.AccountSharedHolder;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;

/**
 *  General Document Factory
 *  ~~~~~~~~~~~~~~~~~~~~~~~~
 */
public class GeneralDocumentFactory implements Document.Factory {

    protected final String type;

    public GeneralDocumentFactory(String docType) {
        super();
        type = docType;
    }

    protected String getType(String docType, ID identifier) {
        if (!docType.equals("*")) {
            return docType;
        } else if (identifier.isGroup()) {
            return Document.BULLETIN;
        } else if (identifier.isUser()) {
            return Document.VISA;
        } else {
            return Document.PROFILE;
        }
    }

    @Override
    public Document createDocument(ID identifier, String data, TransportableData signature) {
        String docType = getType(type, identifier);
        if (data == null || signature == null/* || data.isEmpty() || signature.isEmpty()*/) {
            // create empty document
            if (Document.VISA.equals(docType)) {
                return new BaseVisa(identifier);
            } else if (Document.BULLETIN.equals(docType)) {
                return new BaseBulletin(identifier);
            } else {
                return new BaseDocument(identifier, docType);
            }
        } else {
            // create document with data & signature from local storage
            if (Document.VISA.equals(docType)) {
                return new BaseVisa(identifier, data, signature);
            } else if (Document.BULLETIN.equals(docType)) {
                return new BaseBulletin(identifier, data, signature);
            } else {
                return new BaseDocument(identifier, docType, data, signature);
            }
        }
    }

    @Override
    public Document parseDocument(Map<String, Object> doc) {
        ID identifier = ID.parse(doc.get("ID"));
        if (identifier == null) {
            // assert false : "document ID not found : " + doc;
            return null;
        }
        String docType = AccountSharedHolder.helper.getDocumentType(doc, null);
        if (docType == null) {
            docType = getType("*", identifier);
        }
        if (Document.VISA.equals(docType)) {
            return new BaseVisa(doc);
        } else if (Document.BULLETIN.equals(docType)) {
            return new BaseBulletin(doc);
        } else {
            return new BaseDocument(doc);
        }
    }
}
