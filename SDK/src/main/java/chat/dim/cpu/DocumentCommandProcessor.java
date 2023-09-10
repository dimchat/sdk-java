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
            assert false : "doc ID cannot be empty: " + content;
            return respondReceipt("Document command error.", rMsg, null, null);
        } else if (doc == null) {
            // query entity document for ID
            String type = command.getString("doc_type", "*");
            return getDocument(identifier, type, rMsg);
        } else if (identifier.equals(doc.getIdentifier())) {
            // received a new document for ID
            return putDocument(identifier, command.getMeta(), doc, rMsg);
        }
        // error
        return respondReceipt("Document ID not match.", rMsg, null, null);
    }

    private List<Content> getDocument(ID identifier, String type, ReliableMessage rMsg) {
        Facebook facebook = getFacebook();
        Document doc = facebook.getDocument(identifier, type);
        if (doc == null) {
            return respondReceipt("Document not found.", rMsg, null, newMap(
                    "template", "Document not found: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else {
            Meta meta = facebook.getMeta(identifier);
            return respondContent(DocumentCommand.response(identifier, meta, doc));
        }
    }

    private List<Content> putDocument(ID identifier, Meta meta, Document doc, ReliableMessage rMsg) {
        Facebook facebook = getFacebook();
        // check meta
        if (meta == null) {
            meta = facebook.getMeta(identifier);
            if (meta == null) {
                return respondReceipt("Meta not found.", rMsg, null, newMap(
                        "template", "Meta not found: ${ID}.",
                        "replacements", newMap(
                                "ID", identifier.toString()
                        )
                ));
            }
        } else if (!facebook.saveMeta(meta, identifier)) {
            // meta error
            return respondReceipt("Meta not accepted.", rMsg, null, newMap(
                    "template", "Meta not accepted: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        }
        // check document
        boolean isValid = doc.isValid() || doc.verify(meta.getPublicKey());
        // TODO: check for group document
        if (!isValid) {
            // document error
            return respondReceipt("Document not accepted.", rMsg, null, newMap(
                    "template", "Document not accepted: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else if (facebook.saveDocument(doc)) {
            // document saved
            return respondReceipt("Document received.", rMsg, null, newMap(
                    "template", "Document received: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else {
            // document expired
            return respondReceipt("Document not changed.", rMsg, null, newMap(
                    "template", "Document not changed: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        }
    }

}
