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
public class CustomizedContentProcessor extends BaseContentProcessor {

    public static String FMT_APP_NOT_SUPPORT = "Customized Content (app: %s) not support yet!";
    public static String FMT_ACT_NOT_SUPPORT = "Customized Content (app: %s, act: %s) not support yet!";

    public CustomizedContentProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof CustomizedContent : "customized content error: " + content;
        CustomizedContent customized = (CustomizedContent) content;
        String app = customized.getApplication();
        // check application name
        List<Content> res = check(app, customized, rMsg);
        if (res == null) {
            // check OK, execute the action for sender
            String act = customized.getAction();
            ID sender = rMsg.getSender();
            res = execute(act, sender, customized, rMsg);
        }
        return res;
    }

    // override for your applications
    protected List<Content> check(String app, CustomizedContent content, ReliableMessage rMsg) {
        String text = String.format(FMT_APP_NOT_SUPPORT, app);
        return respondText(text, content.getGroup());
    }

    // override for customized actions
    protected List<Content> execute(String action, ID user, CustomizedContent content, ReliableMessage rMsg) {
        String app = content.getApplication();
        String text = String.format(FMT_ACT_NOT_SUPPORT, app, action);
        return respondText(text, content.getGroup());
    }
}
