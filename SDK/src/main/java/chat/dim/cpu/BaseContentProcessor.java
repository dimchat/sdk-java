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
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.TwinsHelper;
import chat.dim.dkd.ContentProcessor;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;

/**
 *  CPU - Content Processing Unit
 */
public class BaseContentProcessor extends TwinsHelper implements ContentProcessor {

    public BaseContentProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        return respondReceipt("Content not support.", rMsg.getEnvelope(), content, newMap(
                "template", "Content (type: ${type}) not support yet!",
                "replacements", newMap(
                        "type", content.getType()
                )
        ));
    }

    //
    //  Convenient responding
    //

    protected List<Content> respondReceipt(String text, Envelope envelope, Content content, Map<String, Object> extra) {
        // create base receipt command with text & original envelope
        ReceiptCommand res = createReceipt(text, envelope, content, extra);
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }

    /**
     *  Create receipt command with text, original envelope, serial number & group
     *
     * @param text     - text message
     * @param head     - original envelope
     * @param body     - original content
     * @param extra    - extra info
     * @return receipt command
     */
    public static ReceiptCommand createReceipt(String text, Envelope head, Content body, Map<String, Object> extra) {
        assert text != null && head != null : "params error";
        // create base receipt command with text, original envelope, serial number & group ID
        ReceiptCommand res = ReceiptCommand.create(text, head, body);
        // add extra key-value
        if (extra != null) {
            res.putAll(extra);
        }
        return res;
    }

}
