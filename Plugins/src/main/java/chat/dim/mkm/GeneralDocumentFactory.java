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
import chat.dim.plugins.SharedAccountExtensions;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentType;
import chat.dim.protocol.ID;

/**
 *  General Document Factory
 */
public class GeneralDocumentFactory implements Document.Factory {

    protected final String type;

    public GeneralDocumentFactory(String docType) {
        super();
        type = docType;
    }

    protected String getType(String docType, ID identifier) {
        assert docType != null && docType.length() > 0 : "document type empty";
        if (!docType.equals("*")) {
            return docType;
        } else if (identifier.isGroup()) {
            return DocumentType.BULLETIN;
        } else if (identifier.isUser()) {
            return DocumentType.VISA;
        } else {
            return DocumentType.PROFILE;
        }
    }

    @Override
    public Document createDocument(ID identifier, String data, TransportableData signature) {
        String docType = getType(type, identifier);
        if (data == null || data.isEmpty()) {
            assert signature == null : "document error: " + identifier + ", signature: " + signature;
            // create empty document
            switch (docType) {

                case DocumentType.VISA:
                    return new BaseVisa(identifier);

                case DocumentType.BULLETIN:
                    return new BaseBulletin(identifier);

                default:
                    return new BaseDocument(identifier, docType);
            }
        } else {
            assert signature != null : "document error: " + identifier + ", data: " + data;
            // create document with data & signature from local storage
            switch (docType) {

                case DocumentType.VISA:
                    return new BaseVisa(identifier, data, signature);

                case DocumentType.BULLETIN:
                    return new BaseBulletin(identifier, data, signature);

                default:
                    return new BaseDocument(identifier, docType, data, signature);
            }
        }
    }

    @Override
    public Document parseDocument(Map<String, Object> doc) {
        // check 'did', 'data', 'signature'
        ID identifier = ID.parse(doc.get("did"));
        if (identifier == null) {
            assert false : "document ID not found : " + doc;
            return null;
        } else if (doc.get("data") == null || doc.get("signature") == null) {
            // doc.data should not be empty
            // doc.signature should not be empty
            assert false : "document error: " + doc;
            return null;
        }
        String docType = SharedAccountExtensions.helper.getDocumentType(doc, null);
        if (docType == null) {
            docType = getType("*", identifier);
        }
        // create with document type
        switch (docType) {

            case DocumentType.VISA:
                return new BaseVisa(doc);

            case DocumentType.BULLETIN:
                return new BaseBulletin(doc);

            default:
                return new BaseDocument(doc);
        }
    }

}
