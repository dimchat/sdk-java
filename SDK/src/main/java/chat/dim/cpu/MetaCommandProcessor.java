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
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;

public class MetaCommandProcessor extends CommandProcessor {

    public MetaCommandProcessor() {
        super();
    }

    private Content getMeta(ID identifier) {
        Facebook facebook = getFacebook();
        // query meta for ID
        Meta meta = facebook.getMeta(identifier);
        if (meta == null) {
            // meta not found
            String text = String.format("Sorry, meta not found for ID: %s", identifier);
            return new TextContent(text);
        }
        // response
        return new MetaCommand(identifier, meta);
    }

    private Content putMeta(ID identifier, Meta meta) {
        // received a meta for ID
        if (!getFacebook().saveMeta(meta, identifier)) {
            // save meta failed
            String text = String.format("Meta not accept: %s", identifier);
            return new TextContent(text);
        }
        // response
        String text = String.format("Meta received: %s", identifier);
        return new ReceiptCommand(text);
    }

    @Override
    public Content execute(Command cmd, ReliableMessage rMsg) {
        assert cmd instanceof MetaCommand : "meta command error: " + cmd;
        MetaCommand mCmd = (MetaCommand) cmd;
        Meta meta = mCmd.getMeta();
        ID identifier = mCmd.getIdentifier();
        if (meta == null) {
            return getMeta(identifier);
        } else {
            return putMeta(identifier, meta);
        }
    }
}
