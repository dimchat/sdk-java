/* license: https://mit-license.org
 *
 *  Star Gate: Network Connection Module
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
package chat.dim.pack;

import chat.dim.type.ByteArray;

public abstract class PackageSeeker<H, P> {

    private final byte[] MAGIC_CODE;
    private final int MAGIC_OFFSET;
    private final int MAX_HEAD_LENGTH;

    public PackageSeeker(byte[] magicCode, int magicOffset, int maxHeadLen) {
        super();
        MAGIC_CODE = magicCode;
        MAGIC_OFFSET = magicOffset;
        MAX_HEAD_LENGTH = maxHeadLen;
    }

    /**
     *  Get package header from data buffer
     *
     * @param data - data received
     * @return Header
     */
    public abstract H parseHeader(ByteArray data);

    /**
     *  Get length of header
     *
     * @param head - package header
     * @return header length
     */
    public abstract int getHeaderLength(H head);

    /**
     *  Get body length from header
     *
     * @param head - package header
     * @return body length
     */
    public abstract int getBodyLength(H head);

    /**
     *  Create package with buffer, head & body
     *
     * @param data - data buffer
     * @param head - package head
     * @param body - package body
     * @return Package
     */
    public abstract P createPackage(ByteArray data, H head, ByteArray body);

    /**
     *  Seek package header in received data buffer
     *
     * @param data - received data buffer
     * @return header & it's offset, -1 on data error
     */
    public SeekerResult<H> seekHeader(ByteArray data) {
        int dataLen = data.getSize();
        int start = 0;
        int offset;
        int remaining;
        H head;
        while (start < dataLen) {
            // try to parse header
            head = parseHeader(data.slice(start));
            if (head != null) {
                // got header with start position
                return new SeekerResult<>(head, start);
            }
            // header not found, check remaining data
            remaining = dataLen - start;
            if (remaining < MAX_HEAD_LENGTH) {
                // waiting for more data
                break;
            }
            // data error, locate next header
            offset = nextOffset(data, start + 1);
            if (offset < 0) {
                // header not found
                if (remaining < 65536) {
                    // waiting for more data
                    break;
                }
                // skip the whole buffer
                return new SeekerResult<>(null, -1);
            }
            // try again from new offset
            start += offset;
        }
        // header not found, waiting for more data
        return new SeekerResult<>(null, start);
    }

    // locate next header
    private int nextOffset(ByteArray data, int start) {
        start = MAGIC_OFFSET + start;
        int end = start + MAGIC_CODE.length;
        if (end > data.getSize()) {
            // not enough data
            return -1;
        }
        int offset = data.find(MAGIC_CODE, start);
        if (offset < 0) {
            // header not found
            return -1;
        }
        //assert offset > magicOffset : "magic code error: " + data;
        return offset - MAGIC_OFFSET;
    }

    /**
     *  Seek data package from received data buffer
     *
     * @param data - received data buffer
     * @return package & it's offset, -1 on data error
     */
    public SeekerResult<P> seekPackage(ByteArray data) {
        // 1. seek header in received data
        SeekerResult<H> result = seekHeader(data);
        H head = result.value;
        int offset = result.offset;
        if (offset < 0) {
            // data error, ignore the whole buffer
            return new SeekerResult<>(null, -1);
        } else if (head == null) {
            // header not found
            return new SeekerResult<>(null, offset);
        } else if (offset > 0) {
            // skip the error part
            data = data.slice(offset);
        }
        // 2. check length
        int dataLen = data.getSize();
        int headLen = getHeaderLength(head);
        int bodyLen = getBodyLength(head);
        int packLen;
        if (bodyLen < 0) {
            packLen = dataLen;
        } else {
            packLen = headLen + bodyLen;
        }
        // check data buffer
        if (dataLen < packLen) {
            // package not completed, waiting for more data
            return new SeekerResult<>(null, offset);
        } else if (dataLen > packLen) {
            // cut the tail
            data = data.slice(0, packLen);
        }
        // OK
        ByteArray body = data.slice(headLen);
        P pack = createPackage(data, head, body);
        return new SeekerResult<>(pack, offset);
    }
}
