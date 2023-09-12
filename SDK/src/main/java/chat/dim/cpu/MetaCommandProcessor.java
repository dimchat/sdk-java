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
            assert false : "meta ID cannot be empty: " + content;
            return respondReceipt("Meta command error.", rMsg, null, null);
        } else if (meta == null) {
            // query meta for ID
            return getMeta(identifier, rMsg);
        } else {
            // received a meta for ID
            return putMeta(identifier, meta, rMsg);
        }
    }

    private List<Content> getMeta(ID identifier, ReliableMessage rMsg) {
        Meta meta = getFacebook().getMeta(identifier);
        if (meta == null) {
            return respondReceipt("Meta not found.", rMsg, null, newMap(
                    "template", "Meta not found: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else {
            return respondContent(MetaCommand.response(identifier, meta));
        }
    }

    private List<Content> putMeta(ID identifier, Meta meta, ReliableMessage rMsg) {
        Facebook facebook = getFacebook();
        // TODO: check meta
        if (facebook.saveMeta(meta, identifier)) {
            return respondReceipt("Meta received.", rMsg, null, newMap(
                    "template", "Meta received: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        } else {
            return respondReceipt("Meta not accepted.", rMsg, null, newMap(
                    "template", "Meta not accepted: ${ID}.",
                    "replacements", newMap(
                            "ID", identifier.toString()
                    )
            ));
        }
    }

}
