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
import chat.dim.ext.CommandHandler;
import chat.dim.ext.SharedCommandExtensions;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ReliableMessage;

/**
 *  Base implementation of {@link ContentProcessor} with common response utilities.
 *  <p>
 *      Provides default handling for unsupported content types (returns "not supported" receipt)
 *      and utility methods for creating receipt responses. Serves as the parent class
 *      for all concrete content processors.
 *  </p>
 *  <p>
 *      Extends {@link TwinsHelper} to access Facebook (entity management) and Messenger services.
 *  </p>
 */
public class BaseContentProcessor extends TwinsHelper implements ContentProcessor {

    /**
     *  Creates a {@link BaseContentProcessor} with required twin dependencies.
     *
     * @param facebook is the entity management service (user/group operations).
     * @param messenger is the messaging service (packing/processing).
     */
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
    //  Response Utility Methods
    //

    /**
     *  Creates a list containing a single receipt command response.
     *  <p>
     *      Convenience method for consistent response formatting across processors.
     *  </p>
     *
     * @param text is the human-readable response text.
     * @param envelope is the original message envelope (for sender/receiver context).
     * @param content is the original message content (optional, for additional context).
     * @param extra is the extra key-value data to include in the receipt (optional).
     * @return a list with one {@code ReceiptCommand} instance.
     */
    protected List<Content> respondReceipt(String text, Envelope envelope, Content content, Map<String, Object> extra) {
        // create base receipt command with text & original envelope
        Command res = createReceipt(text, envelope, content, extra);
        List<Content> responses = new ArrayList<>();
        responses.add(res);
        return responses;
    }

    /**
     *  Creates a receipt command with standardized formatting.
     *  <p>
     *      Includes original message context (envelope, serial number, group ID)
     *      and optional extra data. Static method for use without instantiation.
     *  </p>
     *
     * @param text is the human-readable response text.
     * @param head is the original message envelope (provides sender/receiver/serial number).
     * @param body is the original message content (optional, for group ID or other context).
     * @param extra is the extra key-value data to add to the receipt (optional).
     * @return a formatted {@code ReceiptCommand} instance.
     */
    public static Command createReceipt(String text, Envelope head, Content body, Map<String, Object> extra) {
        assert text != null && head != null : "params error";
        // create base receipt command with text, original envelope, serial number & group ID
        CommandHandler helper = SharedCommandExtensions.handler;
        Command res = helper.createReceipt(text, head, body);
        // add extra key-value
        if (extra != null) {
            res.putAll(extra);
        }
        return res;
    }

}
