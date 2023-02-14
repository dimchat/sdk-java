/* license: https://mit-license.org
 *
 *  MTP: Message Transfer Protocol
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
package chat.dim.mtp;

import chat.dim.pack.SeekerResult;
import chat.dim.type.ByteArray;

public final class MTPHelper {

    private static final MTPSeeker seeker = new MTPSeeker();

    public static SeekerResult<Header> seekHeader(ByteArray data) {
        return seeker.seekHeader(data);
    }

    public static SeekerResult<Package> seekPackage(ByteArray data) {
        return seeker.seekPackage(data);
    }

    public static Package createCommand(ByteArray body) {
        return Package.create(DataType.COMMAND, null, 1, 0, body.getSize(), body);
    }

    public static Package createMessage(TransactionID sn, ByteArray body) {
        return Package.create(DataType.MESSAGE, sn, 1, 0, body.getSize(), body);
    }

    public static Package respondCommand(TransactionID sn, ByteArray body) {
        return Package.create(DataType.COMMAND_RESPONSE, sn, 1, 0, body.getSize(), body);
    }

    public static Package respondMessage(TransactionID sn, int pages, int index, ByteArray body) {
        return Package.create(DataType.MESSAGE_RESPONSE, sn, pages, index, body.getSize(), body);
    }
}
