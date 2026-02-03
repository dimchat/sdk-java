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
import chat.dim.core.Archivist;
import chat.dim.ext.SharedAccountExtensions;
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
        List<Document> documents = command.getDocuments();
        ID did = command.getIdentifier();
        if (did == null) {
            assert false : "doc ID cannot be empty: " + command;
            return respondReceipt("Document command error.", rMsg.getEnvelope(), command, null);
        } else if (documents == null) {
            // query entity documents for ID
            return getDocuments(did, rMsg.getEnvelope(), command);
        }
        // received new documents
        return putDocuments(did, documents, rMsg.getEnvelope(), command);
    }

    private List<Content> getDocuments(ID did, Envelope envelope, DocumentCommand content) {
        Facebook facebook = getFacebook();
        List<Document> documents = facebook.getDocuments(did);
        if (documents == null || documents.isEmpty()) {
            return respondReceipt("Document not found.", envelope, content, newMap(
                    "template", "Document not found: ${did}.",
                    "replacements", newMap(
                            "did", did.toString()
                    )
            ));
        }
        // documents got
        Date queryTime = content.getLastTime();
        if (queryTime != null) {
            // check last document time
            Document last = getLastDocument(documents);
            assert last != null : "should not happen";
            Date lastTime = last.getTime();
            if (lastTime == null) {
                assert false : "document error: " + last;
            } else if (!lastTime.after(queryTime)) {
                // document not updated
                return respondReceipt("Document not updated.", envelope, content, newMap(
                        "template", "Document not updated: ${did}, last time: ${time}.",
                        "replacements", newMap(
                                "did", did.toString(),
                                "time", lastTime.getTime() / 1000.0d
                        )
                ));
            }
        }
        // documents got
        return respondDocuments(did, documents, envelope.getSender());
    }

    protected List<Content> respondDocuments(ID did, List<Document> documents, ID receiver) {
        assert !receiver.equals(did) : "cycled response: " + did;
        // TODO: check response expired
        Facebook facebook = getFacebook();
        Meta meta = facebook.getMeta(did);
        DocumentCommand res = DocumentCommand.response(did, meta, documents);
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }

    protected Document getLastDocument(Iterable<Document> documents) {
        Document lastDoc = null;
        Date lastTime = null;
        Date docTime;
        for (Document doc : documents) {
            docTime = doc.getTime();
            if (lastDoc == null) {
                // first document
                lastDoc = doc;
                lastTime = docTime;
            } else if (lastTime == null) {
                // the first document has no time (old version),
                // if this document has time, use the new one
                if (docTime != null) {
                    // first document with time
                    lastDoc = doc;
                    lastTime = docTime;
                }
            } else if (docTime != null && docTime.after(lastTime)) {
                // new document
                lastDoc = doc;
                lastTime = docTime;
            }
        }
        return lastDoc;
    }

    private List<Content> putDocuments(ID did, List<Document> documents, Envelope envelope, DocumentCommand content) {
        List<Content> errors;
        Meta meta = content.getMeta();
        // 0. check meta
        if (meta == null) {
            Facebook facebook = getFacebook();
            meta = facebook.getMeta(did);
            if (meta == null) {
                return respondReceipt("Meta not found.", envelope, content, newMap(
                        "template", "Meta not found: ${did}.",
                        "replacements", newMap(
                                "did", did.toString()
                        )
                ));
            }
        } else {
            // 1. try to save meta
            errors = saveMeta(meta, did, envelope, content);
            if (errors != null) {
                // failed
                return errors;
            }
        }
        // 2. try to save documents
        errors = new ArrayList<>();
        List<Content> responses;
        for (Document doc : documents) {
            responses = saveDocument(doc, meta, did, envelope, content);
            if (responses != null) {
                // failed
                errors.addAll(responses);
            }
        }
        if (!errors.isEmpty()) {
            // failed
            return errors;
        }
        // 3. success
        return respondReceipt("Document received.", envelope, content, newMap(
                "template", "Document received: ${did}.",
                "replacements", newMap(
                        "did", did.toString()
                )
        ));
    }

    // return null on success
    protected List<Content> saveDocument(Document doc, Meta meta, ID did, Envelope envelope, DocumentCommand content) {
        Archivist archivist = getArchivist();
        if (archivist == null) {
            assert false : "archivist not ready";
            return null;
        }
        // check document
        if (!checkDocument(doc, meta, did)) {
            // document invalid
            return respondReceipt("Document not accepted.", envelope, content, newMap(
                    "template", "Document not accepted: ${did}.",
                    "replacements", newMap(
                            "did", did.toString()
                    )
            ));
        } else if (!archivist.saveDocument(doc, did)) {
            // document expired
            return respondReceipt("Document not changed.", envelope, content, newMap(
                    "template", "Document not changed: ${did}.",
                    "replacements", newMap(
                            "did", did.toString()
                    )
            ));
        }
        // document saved, return no error
        return null;
    }

    protected boolean checkDocument(Document doc, Meta meta, ID did) {
        // check meta with ID
        if (!checkMeta(meta, did)) {
            // meta error
            return false;
        }
        // check document ID
        ID docID = SharedAccountExtensions.helper.getDocumentID(doc);
        if (docID == null) {
            assert false : "document ID not found: " + doc.toMap();
        } else if (!docID.getAddress().equals(did.getAddress())) {
            assert false : "document ID not matched: " + did + ", " + doc.toMap();
            return false;
        }
        // NOTICE: if this is a bulletin document for group,
        //             verify it with the group owner's meta.key
        //         else (this is a visa document for user)
        //             verify it with the user's meta.key
        return doc.verify(meta.getPublicKey());
        // TODO: check for group document
    }

}
