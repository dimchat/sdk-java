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

    public static String STR_DOC_CMD_ERROR = "Document command error.";
    public static String FMT_DOC_NOT_FOUND = "Sorry, document not found for ID: %s";
    public static String FMT_DOC_NOT_ACCEPTED = "Document not accept: %s";
    public static String FMT_DOC_ACCEPTED = "Document received: %s";

    public DocumentCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    private List<Content> getDocument(ID identifier, String type) {
        Facebook facebook = getFacebook();
        Document doc = facebook.getDocument(identifier, type);
        if (doc == null) {
            String text = String.format(FMT_DOC_NOT_FOUND, identifier);
            return respondText(text, null);
        } else {
            Meta meta = facebook.getMeta(identifier);
            return respondContent(DocumentCommand.response(identifier, meta, doc));
        }
    }

    private List<Content> putDocument(ID identifier, Meta meta, Document doc) {
        Facebook facebook = getFacebook();
        if (meta != null) {
            // received a meta for ID
            if (!facebook.saveMeta(meta, identifier)) {
                String text = String.format(FMT_META_NOT_ACCEPTED, identifier);
                return respondText(text, null);
            }
        }
        // receive a document for ID
        if (facebook.saveDocument(doc))  {
            String text = String.format(FMT_DOC_ACCEPTED, identifier);
            return respondText(text, null);
        } else {
            String text = String.format(FMT_DOC_NOT_ACCEPTED, identifier);
            return respondText(text, null);
        }
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof DocumentCommand : "document command error: " + content;
        DocumentCommand cmd = (DocumentCommand) content;
        ID identifier = cmd.getIdentifier();
        if (identifier != null) {
            Document doc = cmd.getDocument();
            if (doc == null) {
                // query entity document for ID
                String type = (String) cmd.get("doc_type");
                if (type == null) {
                    type = "*";  // ANY
                }
                return getDocument(identifier, type);
            } else if (identifier.equals(doc.getIdentifier())) {
                // received a new document for ID
                return putDocument(identifier, cmd.getMeta(), doc);
            }
        }
        // error
        return respondText(STR_DOC_CMD_ERROR, cmd.getGroup());
    }
}
