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

import java.util.List;

import chat.dim.Facebook;
import chat.dim.Messenger;
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
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof DocumentCommand : "document command error: " + content;
        DocumentCommand command = (DocumentCommand) content;
        ID identifier = command.getIdentifier();
        Document doc = command.getDocument();
        if (identifier == null) {
            assert false : "doc ID cannot be empty: " + command;
            return respondReceipt("Document command error.", rMsg.getEnvelope(), command, null);
        } else if (doc == null) {
            // query entity document for ID
            String type = command.getString("doc_type", "*");
            return getDocument(identifier, type, rMsg.getEnvelope(), command);
        } else if (identifier.equals(doc.getIdentifier())) {
            // received a new document for ID
            return putDocument(identifier, doc, rMsg.getEnvelope(), command);
        }
        // error
        return respondReceipt("Document ID not match.", rMsg.getEnvelope(), command, null);
    }

    private List<Content> getDocument(ID identifier, String type, Envelope envelope, DocumentCommand content) {
        Facebook facebook = getFacebook();
        Document doc = facebook.getDocument(identifier, type);
        if (doc == null) {
            return respondReceipt("Document not found.", envelope, content, newMap(
                    "template", "Document not found: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        }
        // document got
        Meta meta = facebook.getMeta(identifier);
        return respondContent(DocumentCommand.response(identifier, meta, doc));
    }

    private List<Content> putDocument(ID identifier, Document doc, Envelope envelope, DocumentCommand content) {
        Facebook facebook = getFacebook();
        List<Content> errors;
        // 0. check meta
        Meta meta = content.getMeta();
        if (meta == null) {
            meta = facebook.getMeta(identifier);
            if (meta == null) {
                return respondReceipt("Meta not found.", envelope, content, newMap(
                        "template", "Meta not found: ${ID}.",
                        "replacements", newMap(
                                "ID", identifier.toString()
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
                "template", "Document received: ${ID}.",
                "replacements", newMap(
                        "ID", identifier.toString()
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
                    "template", "Document not accepted: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else if (!facebook.saveDocument(doc)) {
            // document expired
            return respondReceipt("Document not changed.", envelope, content, newMap(
                    "template", "Document not changed: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
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
