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
import chat.dim.dkd.BaseMetaCommand;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ReliableMessage;

public class MetaCommandProcessor extends BaseCommandProcessor {

    public static String STR_META_CMD_ERROR = "Meta command error.";
    public static String FMT_META_NOT_FOUND = "Sorry, meta not found for ID: %s";
    public static String FMT_META_NOT_ACCEPTED = "Meta not accepted: %s";
    public static String FMT_META_ACCEPTED = "Meta received: %s";

    public MetaCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    private List<Content> getMeta(ID identifier) {
        Meta meta = getFacebook().getMeta(identifier);
        if (meta == null) {
            String text = String.format(FMT_META_NOT_FOUND, identifier);
            return respondText(text, null);
        } else {
            return respondContent(new BaseMetaCommand(identifier, meta));
        }
    }

    private List<Content> putMeta(ID identifier, Meta meta) {
        if (getFacebook().saveMeta(meta, identifier)) {
            String text = String.format(FMT_META_ACCEPTED, identifier);
            return respondReceipt(text);
        } else {
            String text = String.format(FMT_META_NOT_ACCEPTED, identifier);
            return respondText(text, null);
        }
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof MetaCommand : "meta command error: " + content;
        MetaCommand cmd = (MetaCommand) content;
        Meta meta = cmd.getMeta();
        ID identifier = cmd.getIdentifier();
        if (identifier == null) {
            // error
            return respondText(STR_META_CMD_ERROR, cmd.getGroup());
        } else if (meta == null) {
            // query meta for ID
            return getMeta(identifier);
        } else {
            // received a meta for ID
            return putMeta(identifier, meta);
        }
    }
}
