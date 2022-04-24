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
import chat.dim.TwinsHelper;
import chat.dim.dkd.BaseTextContent;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;

public class BaseContentProcessor extends TwinsHelper implements ContentProcessor {

    public static String FMT_CONTENT_NOT_SUPPORT = "Content (type: %d) not support yet!";

    public BaseContentProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        String text = String.format(FMT_CONTENT_NOT_SUPPORT, content.getType());
        return respondText(text, content.getGroup());
    }

    //
    //  Convenient responding
    //
    protected List<Content> respondText(String text, ID group) {
        TextContent res = new BaseTextContent(text);
        if (group != null) {
            res.setGroup(group);
        }
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }
    protected List<Content> respondReceipt(String text) {
        ReceiptCommand res = new ReceiptCommand(text);
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }
    protected List<Content> respondContent(Content res) {
        if (res == null) {
            return null;
        }
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }
}
