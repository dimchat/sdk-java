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
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ReliableMessage;

public class MetaCommandProcessor extends BaseCommandProcessor {

    public MetaCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof MetaCommand : "meta command error: " + content;
        MetaCommand command = (MetaCommand) content;
        Meta meta = command.getMeta();
        ID identifier = command.getIdentifier();
        if (identifier == null) {
            assert false : "meta ID cannot be empty: " + command;
            return respondReceipt("Meta command error.", rMsg.getEnvelope(), command, null);
        } else if (meta == null) {
            // query meta for ID
            return getMeta(identifier, rMsg.getEnvelope(), command);
        } else {
            // received a meta for ID
            return putMeta(identifier, meta, rMsg.getEnvelope(), command);
        }
    }

    private List<Content> getMeta(ID identifier, Envelope envelope, Content content) {
        Meta meta = getFacebook().getMeta(identifier);
        if (meta == null) {
            return respondReceipt("Meta not found.", envelope, content, newMap(
                    "template", "Meta not found: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else {
            return respondContent(MetaCommand.response(identifier, meta));
        }
    }

    private List<Content> putMeta(ID identifier, Meta meta, Envelope envelope, Content content) {
        List<Content> errors;
        // 1. try to save meta
        errors = saveMeta(identifier, meta, envelope, content);
        if (errors != null) {
            // failed
            return errors;
        }
        // 2. success
        return respondReceipt("Meta received.", envelope, content, newMap(
                "template", "Meta received: ${ID}.",
                "replacements", newMap(
                        "ID", identifier.toString()
                )
        ));
    }

    // return null on success
    protected List<Content> saveMeta(ID identifier, Meta meta, Envelope envelope, Content content) {
        Facebook facebook = getFacebook();
        // check meta
        if (!meta.isValid() || !meta.matchIdentifier(identifier)) {
            // meta invalid
            return respondReceipt("Meta not valid.", envelope, content, newMap(
                    "template", "Meta not valid: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else if (!facebook.saveMeta(meta, identifier)) {
            // DB error?
            return respondReceipt("Meta not accepted.", envelope, content, newMap(
                    "template", "Meta not accepted: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        }
        // OK
        return null;
    }

}
