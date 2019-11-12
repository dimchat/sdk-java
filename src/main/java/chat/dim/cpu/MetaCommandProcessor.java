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

import java.util.Locale;

import chat.dim.Messenger;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.TextContent;

public class MetaCommandProcessor extends CommandProcessor {

    public MetaCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content getMeta(ID identifier) {
        // query meta for ID
        Meta meta = getFacebook().getMeta(identifier);
        if (meta == null) {
            String text = String.format(Locale.CHINA, "Sorry, meta for %s not found", identifier);
            return new TextContent(text);
        } else {
            return new MetaCommand(identifier, meta);
        }
    }

    private Content putMeta(ID identifier, Meta meta) {
        // received a meta for ID
        if (getFacebook().saveMeta(meta, identifier)) {
            String text = String.format(Locale.CHINA, "Meta saved for %s", identifier);
            return new ReceiptCommand(text);
        } else {
            String text = String.format(Locale.CHINA, "Sorry, meta for %s error", identifier);
            return new TextContent(text);
        }
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof MetaCommand;
        MetaCommand cmd = (MetaCommand) content;
        Meta meta;
        try {
            meta = cmd.getMeta();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        ID identifier = getFacebook().getID(cmd.getIdentifier());
        if (meta == null) {
            return getMeta(identifier);
        } else {
            return putMeta(identifier, meta);
        }
    }
}
