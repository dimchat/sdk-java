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

import chat.dim.Messenger;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.VideoContent;

public class VideoContentProcessor extends ContentProcessor {

    public VideoContentProcessor(Messenger messenger) {
        super(messenger);
    }

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof VideoContent;
        VideoContent video = (VideoContent) content;

        // build text
        if (content.get("text") == null) {
            String text = String.format("[Movie:%s]", video.getFilename());
            content.put("text", text);
        }

        // response
        Object group = content.getGroup();
        if (group == null) {
            return new ReceiptCommand("Movie received", iMsg.envelope, content.serialNumber);
        }
        // no need to response group message
        return null;
    }
}
