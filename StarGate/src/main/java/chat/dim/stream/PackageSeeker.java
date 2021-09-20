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
package chat.dim.stream;

import chat.dim.type.ByteArray;

public abstract class PackageSeeker<H, P> {

    private final byte[] MAGIC_CODE;
    private final int MAGIC_CODE_OFFSET;
    private final int MAX_HEAD_LENGTH;

    public PackageSeeker(byte[] magicCode, int magicOffset, int maxHeadLen) {
        super();
        MAGIC_CODE = magicCode;
        MAGIC_CODE_OFFSET = magicOffset;
        MAX_HEAD_LENGTH = maxHeadLen;
    }

    /**
     *  Get package header from data buffer
     *
     * @param data - received data
     * @return header
     */
    public abstract H parseHeader(ByteArray data);

    /**
     *  Get length of header
     *
     * @param head - header
     * @return header length
     */
    protected abstract int getHeadLength(H head);

    /**
     *  Get body length from header
     *
     * @param head - header
     * @return body length
     */
    protected abstract int getBodyLength(H head);

    /**
     *  Create package with buffer, head & body
     *
     * @param data - buffer
     * @param head - header
     * @param body - body
     * @return package
     */
    protected abstract P newPackage(ByteArray data, H head, ByteArray body);

    /**
     *  Seek package header in received data buffer
     *
     * @param data - data buffer
     * @return header and it's offset, -1 on data error
     */
    public SeekerResult<H> seekHeader(ByteArray data) {
        H head = parseHeader(data);
        if (head != null) {
            // got it (offset = 0)
            return new SeekerResult<>(head, 0);
        }
        int dataLen = data.getSize();
        if (dataLen < MAX_HEAD_LENGTH) {
            // waiting for more data
            return new SeekerResult<>(null, 0);
        }
        // locate next header
        int offset = data.find(MAGIC_CODE, MAGIC_CODE_OFFSET + 1);
        if (offset < 0) {
            if (dataLen < 65536) {
                // waiting for more data
                return new SeekerResult<>(null, 0);
            }
            // skip the whole buffer
            return new SeekerResult<>(null, -1);
        }
        assert offset > MAGIC_CODE_OFFSET : "magic code error";
        // found next header, skip data before it
        offset -= MAGIC_CODE_OFFSET;
        data = data.slice(offset);
        // try again from new offset
        head = parseHeader(data);
        return new SeekerResult<>(head, offset);
    }

    /**
     *  Seek data package from received data buffer
     *
     * @param data - data buffer
     * @return package and it's offset, -1 on data error
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
            // drop the error part
            data = data.slice(offset);
        }
        // 2. check length
        int dataLen = data.getSize();
        int headLen = getHeadLength(head);
        int bodyLen = getBodyLength(head);
        int packLen;
        if (bodyLen < 0) {
            packLen = dataLen;
        } else {
            packLen = headLen + bodyLen;
        }

        if (dataLen < packLen) {
            // package not completed, waiting for more data
            return new SeekerResult<>(null, offset);
        } else if (dataLen > packLen) {
            // cut the tail
            data = data.slice(0, packLen);
        }
        // OK
        ByteArray body = data.slice(headLen);
        P pack = newPackage(data, head, body);
        return new SeekerResult<>(pack, offset);
    }
}
