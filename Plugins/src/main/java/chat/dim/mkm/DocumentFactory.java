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

import chat.dim.protocol.Document;
import chat.dim.protocol.ID;

final class DocumentFactory implements Document.Factory {

    private final String version;

    DocumentFactory(String type) {
        super();
        version = type;
    }

    private String getType(ID identifier) {
        if (version.equals("*")) {
            if (identifier.isGroup()) {
                return Document.BULLETIN;
            }
            if (identifier.isUser()) {
                return Document.VISA;
            }
            return Document.PROFILE;
        }
        return version;
    }

    @Override
    public Document createDocument(ID identifier, byte[] data, byte[] signature) {
        String type = getType(identifier);
        if (Document.VISA.equals(type)) {
            return new BaseVisa(identifier, data, signature);
        }
        if (Document.BULLETIN.equals(type)) {
            return new BaseBulletin(identifier, data, signature);
        }
        return new BaseDocument(identifier, data, signature);
    }

    @Override
    public Document createDocument(ID identifier) {
        String type = getType(identifier);
        if (Document.VISA.equals(type)) {
            return new BaseVisa(identifier);
        }
        if (Document.BULLETIN.equals(type)) {
            return new BaseBulletin(identifier);
        }
        return new BaseDocument(identifier, type);
    }

    @Override
    public Document parseDocument(Map<String, Object> doc) {
        ID identifier = Document.getIdentifier(doc);
        if (identifier == null) {
            return null;
        }
        String type = Document.getType(doc);
        if (type == null) {
            if (identifier.isGroup()) {
                type = Document.BULLETIN;
            } else {
                type = Document.VISA;
            }
        }
        if (Document.VISA.equals(type)) {
            return new BaseVisa(doc);
        }
        if (Document.BULLETIN.equals(type)) {
            return new BaseBulletin(doc);
        }
        return new BaseDocument(doc);
    }
}
