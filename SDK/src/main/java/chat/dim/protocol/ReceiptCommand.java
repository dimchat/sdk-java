/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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
package chat.dim.protocol;

import java.util.Date;
import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,  // the same serial number with the original message
 *
 *      command  : "receipt",
 *      message  : "...",
 *      // -- extra info
 *      sender   : "...",
 *      receiver : "...",
 *      time     : 0
 *  }
 */
public class ReceiptCommand extends Command {

    // original message info
    private Envelope envelope;

    public ReceiptCommand(Map<String, Object> dictionary) {
        super(dictionary);
        envelope = null;
    }

    public ReceiptCommand(String message, long sn, Envelope env) {
        super(RECEIPT);
        // text
        if (message != null) {
            put("message", message);
        }
        // sn of the message responding to
        if (sn > 0) {
            put("sn", sn);
        }
        // envelope of the message responding to
        if (env != null) {
            put("sender", env.getSender().toString());
            put("receiver", env.getReceiver().toString());
            Date time = env.getTime();
            if (time != null) {
                put("time", time.getTime() / 1000);
            }
        }
        envelope = env;
    }

    public ReceiptCommand(String message) {
        this(message, 0, null);
    }

    //-------- setters/getters --------

    public String getMessage() {
        return (String) get("message");
    }

    @SuppressWarnings("unchecked")
    public Envelope getEnvelope() {
        if (envelope == null) {
            // envelope: { sender: "...", receiver: "...", time: 0 }
            Object env = get("envelope");
            if (env == null) {
                Object sender = get("sender");
                Object receiver = get("receiver");
                if (sender != null && receiver != null) {
                    env = getMap();
                }
            }
            if (env instanceof Map) {
                envelope = Envelope.parse((Map<String, Object>) env);
            }
        }
        return envelope;
    }
}
