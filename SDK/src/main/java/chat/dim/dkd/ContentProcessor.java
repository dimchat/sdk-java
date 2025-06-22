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
package chat.dim.dkd;

import java.util.List;

import chat.dim.protocol.Content;
import chat.dim.protocol.ReliableMessage;

/**
 *  CPU - Content Processing Unit
 */
public interface ContentProcessor {

    /**
     *  Process message content
     *
     * @param content - content received
     * @param rMsg    - reliable message
     * @return {Content} response to sender
     */
    List<Content> processContent(Content content, ReliableMessage rMsg);

    /**
     *  CPU Creator
     */
    interface Creator {

        /**
         *  Create content processor with type
         *
         * @param msgType - content type
         * @return ContentProcessor
         */
        ContentProcessor createContentProcessor(String msgType);

        /**
         *  Create command processor with name
         *
         * @param msgType - content type
         * @param cmdName - command name
         * @return CommandProcessor
         */
        ContentProcessor createCommandProcessor(String msgType, String cmdName);
    }

    /**
     *  CPU Factory
     */
    interface Factory {

        /**
         *  Get content/command processor
         *
         * @param content - Content/Command
         * @return ContentProcessor
         */
        ContentProcessor getContentProcessor(Content content);

        ContentProcessor getContentProcessor(String msgType);
    }
}
