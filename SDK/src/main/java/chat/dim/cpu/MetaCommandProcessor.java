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

import chat.dim.Messenger;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ReliableMessage;

public class MetaCommandProcessor extends CommandProcessor {

    public static String STR_META_CMD_ERROR = "Meta command error.";
    public static String FMT_META_NOT_FOUND = "Sorry, meta not found for ID: %s";
    public static String FMT_META_NOT_ACCEPTED = "Meta not accepted: %s";
    public static String FMT_META_ACCEPTED = "Meta received: %s";

    public MetaCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private List<Content> getMeta(final ID identifier) {
        final Meta meta = getFacebook().getMeta(identifier);
        if (meta == null) {
            final String text = String.format(FMT_META_NOT_FOUND, identifier);
            return respondText(text, null);
        } else {
            return respondContent(new MetaCommand(identifier, meta));
        }
    }

    private List<Content> putMeta(final ID identifier, final Meta meta) {
        if (getFacebook().saveMeta(meta, identifier)) {
            final String text = String.format(FMT_META_ACCEPTED, identifier);
            return respondReceipt(text);
        } else {
            final String text = String.format(FMT_META_NOT_ACCEPTED, identifier);
            return respondText(text, null);
        }
    }

    @Override
    public List<Content> execute(final Command cmd, final ReliableMessage rMsg) {
        assert cmd instanceof MetaCommand : "meta command error: " + cmd;
        final MetaCommand mCmd = (MetaCommand) cmd;
        final Meta meta = mCmd.getMeta();
        final ID identifier = mCmd.getIdentifier();
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
