/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
import chat.dim.protocol.Content;
import chat.dim.protocol.CustomizedContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;

/**
 *  Customized Content Processing Unit
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
public class CustomizedContentProcessor extends BaseContentProcessor implements CustomizedContentHandler {

    public CustomizedContentProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        assert content instanceof CustomizedContent : "customized content error: " + content;
        CustomizedContent customized = (CustomizedContent) content;
        // 1. check app id
        String app = customized.getApplication();
        List<Content> res = filter(app, customized, rMsg);
        if (res != null) {
            // app id not found
            return res;
        }
        // 2. get handler with module name
        String mod = customized.getModule();
        CustomizedContentHandler handler = fetch(mod, customized, rMsg);
        if (handler == null) {
            // module not support
            return null;
        }
        // 3. do the job
        String act = customized.getAction();
        ID sender = rMsg.getSender();
        return handler.handleAction(act, sender, customized, rMsg);
    }

    // override for your application
    protected List<Content> filter(String app, CustomizedContent content, ReliableMessage rMsg) {
        return respondReceipt("Content not support.", rMsg.getEnvelope(), content, newMap(
                "template", "Customized content (app: ${app}) not support yet!",
                "replacements", newMap(
                        "app", app
                )
        ));
    }

    // override for your module
    protected CustomizedContentHandler fetch(String mod, CustomizedContent content, ReliableMessage rMsg) {
        // if the application has too many modules, I suggest you to
        // use different handler to do the jobs for each module.
        return this;
    }

    // override for customized actions
    @Override
    public List<Content> handleAction(String act, ID sender, CustomizedContent content, ReliableMessage rMsg) {
        String app = content.getApplication();
        String mod = content.getModule();
        return respondReceipt("Content not support.", rMsg.getEnvelope(), content, newMap(
                "template", "Customized content (app: ${app}, mod: ${mod}, act: ${act}) not support yet!",
                "replacements", newMap(
                        "app", app,
                        "mod", mod,
                        "act", act
                )
        ));
    }
}
