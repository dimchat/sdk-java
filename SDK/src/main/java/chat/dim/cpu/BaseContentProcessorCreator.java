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

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.TwinsHelper;
import chat.dim.dkd.ContentProcessor;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;

/**
 *  Base ContentProcessor Creator
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
public class BaseContentProcessorCreator extends TwinsHelper implements ContentProcessor.Creator {

    public BaseContentProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public ContentProcessor createContentProcessor(int msgType) {

        // forward content
        if (ContentType.FORWARD.equals(msgType)) {
            return new ForwardContentProcessor(getFacebook(), getMessenger());
        }

        // array content
        if (ContentType.ARRAY.equals(msgType)) {
            return new ArrayContentProcessor(getFacebook(), getMessenger());
        }

        // default commands
        if (ContentType.COMMAND.equals(msgType)) {
            return new BaseCommandProcessor(getFacebook(), getMessenger());
        }

        //if (ContentType.ANY.equals(msgType)) {
        if (msgType == 0) {
            // must return a default processor for type==0
            return new BaseContentProcessor(getFacebook(), getMessenger());
        }

        // unknown content
        return null;
    }

    @Override
    public ContentProcessor createCommandProcessor(int msgType, String cmdName) {
        switch (cmdName) {

            // meta command
            case Command.META:
                return new MetaCommandProcessor(getFacebook(), getMessenger());

            // document command
            case Command.DOCUMENT:
                return new DocumentCommandProcessor(getFacebook(), getMessenger());

            // unknown command
            default:
                return null;
        }
    }

}
