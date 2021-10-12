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
import java.util.List;

import chat.dim.Facebook;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;

public class DocumentCommandProcessor extends CommandProcessor {

    public DocumentCommandProcessor() {
        super();
    }

    private Content getDocument(final ID identifier, final String type) {
        final Facebook facebook = getFacebook();
        // query entity document for ID
        final Document doc = facebook.getDocument(identifier, type);
        if (doc == null) {
            // document not found
            final String text = String.format("Sorry, document not found for ID: %s", identifier);
            return new TextContent(text);
        }
        // response
        final Meta meta = facebook.getMeta(identifier);
        return new DocumentCommand(identifier, meta, doc);
    }

    private Content putDocument(final ID identifier, final Meta meta, final Document doc) {
        final Facebook facebook = getFacebook();
        if (meta != null) {
            // received a meta for ID
            if (!facebook.saveMeta(meta, identifier)) {
                // meta not match
                final String text = String.format("Meta not accept: %s", identifier);
                return new TextContent(text);
            }
        }
        // receive a document for ID
        if (!facebook.saveDocument(doc))  {
            // save document failed
            final String text = String.format("Document not accept: %s", identifier);
            return new TextContent(text);
        }
        // response
        final String text = String.format("Document received: %s", identifier);
        return new ReceiptCommand(text);
    }

    @Override
    public List<Content> execute(final Command cmd, final ReliableMessage rMsg) {
        assert cmd instanceof DocumentCommand : "document command error: " + cmd;
        final DocumentCommand dCmd = (DocumentCommand) cmd;
        Content res = null;
        final ID identifier = dCmd.getIdentifier();
        if (identifier != null) {
            final Document doc = dCmd.getDocument();
            if (doc == null) {
                String type = (String) cmd.get("doc_type");
                if (type == null) {
                    type = "*";  // ANY
                }
                res = getDocument(identifier, type);
            } else if (identifier.equals(doc.getIdentifier())) {
                // check meta
                final Meta meta = dCmd.getMeta();
                res = putDocument(identifier, meta, doc);
            }
        }
        if (res == null) {
            // command error
            return null;
        }
        final List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }
}
