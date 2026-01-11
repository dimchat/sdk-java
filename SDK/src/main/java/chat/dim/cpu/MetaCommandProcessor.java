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
import chat.dim.Messenger;
import chat.dim.core.Archivist;
import chat.dim.protocol.Address;
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

    protected Archivist getArchivist() {
        Facebook facebook = getFacebook();
        return facebook == null ? null : facebook.getArchivist();
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        assert content instanceof MetaCommand : "meta command error: " + content;
        MetaCommand command = (MetaCommand) content;
        Meta meta = command.getMeta();
        ID did = command.getIdentifier();
        if (did == null) {
            assert false : "meta ID cannot be empty: " + command;
            return respondReceipt("Meta command error.", rMsg.getEnvelope(), command, null);
        } else if (meta == null) {
            // query meta for ID
            return getMeta(did, rMsg.getEnvelope(), command);
        }
        // received a meta for ID
        return putMeta(meta, did, rMsg.getEnvelope(), command);
    }

    private List<Content> getMeta(ID did, Envelope envelope, MetaCommand content) {
        Facebook facebook = getFacebook();
        Meta meta = facebook.getMeta(did);
        if (meta == null) {
            return respondReceipt("Meta not found.", envelope, content, newMap(
                    "template", "Meta not found: ${did}.",
                    "replacements", newMap(
                            "did", did.toString()
                    )
            ));
        }
        // meta got
        return respondMeta(did, meta, envelope.getSender());
    }

    protected List<Content> respondMeta(ID did, Meta meta, ID receiver) {
        assert receiver.equals(did) : "cycled response";
        // TODO: check response expired
        MetaCommand res = MetaCommand.response(did, meta);
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }

    private List<Content> putMeta(Meta meta, ID did, Envelope envelope, MetaCommand content) {
        List<Content> errors;
        // 1. try to save meta
        errors = saveMeta(meta, did, envelope, content);
        if (errors != null) {
            // failed
            return errors;
        }
        // 2. success
        return respondReceipt("Meta received.", envelope, content, newMap(
                "template", "Meta received: ${did}.",
                "replacements", newMap(
                        "did", did.toString()
                )
        ));
    }

    // return null on success
    protected List<Content> saveMeta(Meta meta, ID did, Envelope envelope, MetaCommand content) {
        Archivist archivist = getArchivist();
        if (archivist == null) {
            assert false : "archivist not ready";
            return null;
        }
        // check meta
        if (!checkMeta(meta, did)) {
            // meta invalid
            return respondReceipt("Meta not valid.", envelope, content, newMap(
                    "template", "Meta not valid: ${did}.",
                    "replacements", newMap(
                            "did", did.toString()
                    )
            ));
        } else if (!archivist.saveMeta(meta, did)) {
            // DB error?
            return respondReceipt("Meta not accepted.", envelope, content, newMap(
                    "template", "Meta not accepted: ${did}.",
                    "replacements", newMap(
                            "did", did.toString()
                    )
            ));
        }
        // meta saved, return no error
        return null;
    }

    protected boolean checkMeta(Meta meta, ID did) {
        if (!meta.isValid()) {
            return false;
        }
        Address old = did.getAddress();
        Address gen = Address.generate(meta, old.getNetwork());
        return old.equals(gen);
    }

}
