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
import chat.dim.protocol.Content;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class ForwardContentProcessor extends BaseContentProcessor {

    public ForwardContentProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof ForwardContent : "forward content error: " + content;
        ForwardContent forward = (ForwardContent) content;
        List<ReliableMessage> secrets = forward.getSecrets();
        // call messenger to process it
        Messenger messenger = getMessenger();
        List<Content> responses = new ArrayList<>();
        List<Content> results;
        for (ReliableMessage item : secrets) {
            // 1. verify message
            SecureMessage sMsg = messenger.verifyMessage(item);
            if (sMsg == null) {
                // waiting for sender's meta if not exists
                continue;
            }
            // 2. decrypt message
            InstantMessage iMsg = messenger.decryptMessage(sMsg);
            if (iMsg == null) {
                // NOTICE: decrypt failed, not for you?
                //         it means you are asked to re-pack and forward this message
                continue;
            }
            // 3. process message content
            results = messenger.processContent(iMsg.getContent(), item);
            if (results != null/* && results.size() > 0*/) {
                responses.addAll(results);
            }
        }
        return responses.size() > 0 ? responses : null;
    }
}
