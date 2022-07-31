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
import chat.dim.protocol.ReliableMessage;

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
        Content res;
        List<ReliableMessage> results;
        for (ReliableMessage item : secrets) {
            results = messenger.processMessage(item);
            if (results == null) {
               res = ForwardContent.create(new ArrayList<>());
            } else if (results.size() == 1) {
                res = ForwardContent.create(results.get(0));
            } else {
                res = ForwardContent.create(results);
            }
            responses.add(res);
        }
        return responses.size() > 0 ? responses : null;
    }
}
