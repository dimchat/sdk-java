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
package chat.dim.cpu;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.mkm.DocumentUtils;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;

public class DocumentCommandProcessor extends MetaCommandProcessor {

    public DocumentCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }
    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        assert content instanceof DocumentCommand : "document command error: " + content;
        DocumentCommand command = (DocumentCommand) content;
        ID identifier = command.getIdentifier();
        Document doc = command.getDocument();
        if (identifier == null) {
            assert false : "doc ID cannot be empty: " + command;
            return respondReceipt("Document command error.", rMsg.getEnvelope(), command, null);
        } else if (doc == null) {
            // query entity documents for ID
            return getDocuments(identifier, rMsg.getEnvelope(), command);
        } else if (identifier.equals(doc.getIdentifier())) {
            // received a new document for ID
            return putDocument(identifier, doc, rMsg.getEnvelope(), command);
        }
        // error
        return respondReceipt("Document ID not match.", rMsg.getEnvelope(), command, newMap(
                "template", "Document ID not match: ${did}.",
                "replacements", newMap(
                        "did", identifier.toString()
                )
        ));
    }

    private List<Content> getDocuments(ID identifier, Envelope envelope, DocumentCommand content) {
        Facebook facebook = getFacebook();
        List<Document> documents = facebook.getDocuments(identifier);
        if (documents == null || documents.isEmpty()) {
            return respondReceipt("Document not found.", envelope, content, newMap(
                    "template", "Document not found: ${did}.",
                    "replacements", newMap(
                            "did", identifier.toString()
                    )
            ));
        }
        // documents got
        Date queryTime = content.getLastTime();
        if (queryTime != null) {
            // check last document time
            Document last = DocumentUtils.lastDocument(documents, null);
            assert last != null : "should not happen";
            Date lastTime = last.getTime();
            if (lastTime == null) {
                assert false : "document error: " + last;
            } else if (!lastTime.after(queryTime)) {
                // document not updated
                return respondReceipt("Document not updated.", envelope, content, newMap(
                        "template", "Document not updated: ${did}, last time: ${time}.",
                        "replacements", newMap(
                                "did", identifier.toString(),
                                "time", lastTime.getTime() / 1000.0d
                        )
                ));
            }
        }
        Meta meta = facebook.getMeta(identifier);
        List<Content> responses = new ArrayList<>();
        // respond first document with meta
        DocumentCommand command = DocumentCommand.response(identifier, meta, documents.get(0));
        responses.add(command);
        for (int i = 1; i < documents.size(); ++i) {
            // respond other documents
            command = DocumentCommand.response(identifier, documents.get(i));
            responses.add(command);
        }
        return responses;
    }

    private List<Content> putDocument(ID identifier, Document doc, Envelope envelope, DocumentCommand content) {
        Facebook facebook = getFacebook();
        List<Content> errors;
        Meta meta = content.getMeta();
        // 0. check meta
        if (meta == null) {
            meta = facebook.getMeta(identifier);
            if (meta == null) {
                return respondReceipt("Meta not found.", envelope, content, newMap(
                        "template", "Meta not found: ${did}.",
                        "replacements", newMap(
                                "did", identifier.toString()
                        )
                ));
            }
        } else {
            // 1. try to save meta
            errors = saveMeta(meta, identifier, envelope, content);
            if (errors != null) {
                // failed
                return errors;
            }
        }
        // 2. try to save document
        errors = saveDocument(doc, meta, identifier, envelope, content);
        if (errors != null) {
            // failed
            return errors;
        }
        // 3. success
        return respondReceipt("Document received.", envelope, content, newMap(
                "template", "Document received: ${did}.",
                "replacements", newMap(
                        "did", identifier.toString()
                )
        ));
    }

    // return null on success
    protected List<Content> saveDocument(Document doc, Meta meta, ID identifier, Envelope envelope, DocumentCommand content) {
        Facebook facebook = getFacebook();
        // check document
        if (!checkDocument(doc, meta)) {
            // document invalid
            return respondReceipt("Document not accepted.", envelope, content, newMap(
                    "template", "Document not accepted: ${did}.",
                    "replacements", newMap(
                            "did", identifier.toString()
                    )
            ));
        } else if (!facebook.saveDocument(doc)) {
            // document expired
            return respondReceipt("Document not changed.", envelope, content, newMap(
                    "template", "Document not changed: ${did}.",
                    "replacements", newMap(
                            "did", identifier.toString()
                    )
            ));
        }
        // document saved, return no error
        return null;
    }

    protected boolean checkDocument(Document doc, Meta meta) {
        if (doc.isValid()) {
            return true;
        }
        // NOTICE: if this is a bulletin document for group,
        //             verify it with the group owner's meta.key
        //         else (this is a visa document for user)
        //             verify it with the user's meta.key
        return doc.verify(meta.getPublicKey());
        // TODO: check for group document
    }

}
