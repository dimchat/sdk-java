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

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;

public class DocumentCommandProcessor extends CommandProcessor {

    public DocumentCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content getDocument(ID identifier, String type) {
        Facebook facebook = getFacebook();
        // query entity document for ID
        Document doc = facebook.getDocument(identifier, type);
        if (facebook.isEmpty(doc)) {
            // document not found
            String text = String.format("Sorry, document not found for ID: %s", identifier);
            return new TextContent(text);
        }
        // response
        return new DocumentCommand(identifier, doc);
    }

    private Content putDocument(ID identifier, Meta meta, Document doc) {
        Facebook facebook = getFacebook();
        if (meta != null) {
            // received a meta for ID
            if (!meta.matches(identifier)) {
                // meta not match
                String text = String.format("Meta not match ID: %s", identifier);
                return new TextContent(text);
            }
            if (!facebook.saveMeta(meta, identifier)) {
                // meta not match
                String text = String.format("Meta not accept: %s", identifier);
                return new TextContent(text);
            }
        }
        // receive a document for ID
        if (!facebook.verify(doc, identifier)) {
            // document signature not match
            String text = String.format("document ID not match: %s", identifier);
            return new TextContent(text);
        }
        if (!facebook.saveDocument(doc))  {
            // save document failed
            String text = String.format("document not accept: %s", identifier);
            return new TextContent(text);
        }
        // response
        String text = String.format("document received: %s", identifier);
        return new ReceiptCommand(text);
    }

    @Override
    public Content process(Content content, ReliableMessage rMsg) {
        assert content instanceof DocumentCommand : "document command error: " + content;
        DocumentCommand cmd = (DocumentCommand) content;
        Document doc = cmd.getDocument();
        ID identifier = cmd.getIdentifier();
        if (doc == null) {
            String type = (String) cmd.get("doc_type");
            if (type == null) {
                type = Document.ANY;
            }
            return getDocument(identifier, type);
        } else {
            // check meta
            Meta meta = cmd.getMeta();
            return putDocument(identifier, meta, doc);
        }
    }
}
