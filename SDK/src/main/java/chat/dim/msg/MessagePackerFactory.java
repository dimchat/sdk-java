/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2026 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Albert Moky
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
package chat.dim.msg;

/**
 * Factory for creating message packers.
 *
 * Provides creation methods for the three message packers
 * (instant/secure/reliable), which can be overridden by subclasses.
 */
public class MessagePackerFactory {

    /**
     * Creates an {@link InstantMessagePacker} for the given delegate.
     *
     * @param delegate the instant message delegate (encryption pipeline)
     * @return a new {@link InstantMessagePacker} instance
     */
    public InstantMessagePacker createInstantMessagePacker(InstantMessageDelegate delegate) {
        return new InstantMessagePacker(delegate);
    }

    /**
     * Creates a {@link SecureMessagePacker} for the given delegate.
     *
     * @param delegate the secure message delegate (decryption/signing pipeline)
     * @return a new {@link SecureMessagePacker} instance
     */
    public SecureMessagePacker  createSecureMessagePacker(SecureMessageDelegate delegate) {
        return new SecureMessagePacker(delegate);
    }

    /**
     * Creates a {@link ReliableMessagePacker} for the given delegate.
     *
     * @param delegate the reliable message delegate (verification pipeline)
     * @return a new {@link ReliableMessagePacker} instance
     */
    public ReliableMessagePacker createReliableMessagePacker(ReliableMessageDelegate delegate) {
        return new ReliableMessagePacker(delegate);
    }

}
