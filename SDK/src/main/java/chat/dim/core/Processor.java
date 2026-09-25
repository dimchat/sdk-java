/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim.core;

import java.util.List;

import chat.dim.protocol.Content;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

// -----------------------------------------------------------------------------
//  Message Processor (Processing Pipeline)
// -----------------------------------------------------------------------------

/**
 *  Message processing interface (handles received messages and generates responses).
 *  <p>
 *      Processes messages through a layered pipeline:
 *      Binary package &rarr; ReliableMessage &rarr; SecureMessage &rarr; InstantMessage &rarr; Content
 *  </p>
 *  <p>
 *      Generates response messages by reversing the pipeline.
 *  </p>
 */
public interface Processor {

    /**
     *  Processes a binary data package to generate response packages.
     *
     * @param data is the binary data package to process (received from network).
     * @return the list of binary response packages (empty if no response needed).
     */
    List<byte[]> processPackage(byte[] data);

    /**
     *  Processes a reliable message to generate response reliable messages.
     *
     * @param rMsg is the reliable message to process (after deserialization).
     * @return the list of reliable response messages (empty if no response needed).
     */
    List<ReliableMessage> processReliableMessage(ReliableMessage rMsg);

    /**
     *  Processes a secure message to generate response secure messages.
     *
     * @param sMsg is the secure message to process (after verification).
     * @param rMsg is the original reliable message (for context).
     * @return the list of secure response messages (empty if no response needed).
     */
    List<SecureMessage> processSecureMessage(SecureMessage sMsg, ReliableMessage rMsg);

    /**
     *  Processes a plain instant message to generate response instant messages.
     *
     * @param iMsg is the instant message to process (after decryption).
     * @param rMsg is the original reliable message (for context).
     * @return the list of instant response messages (empty if no response needed).
     */
    List<InstantMessage> processInstantMessage(InstantMessage iMsg, ReliableMessage rMsg);

    /**
     *  Processes message content to generate response content items.
     *
     * @param content is the message content to process (extracted from instant message).
     * @param rMsg is the original reliable message (for context).
     * @return the list of response content items (empty if no response needed).
     */
    List<Content> processContent(Content content, ReliableMessage rMsg);
}
