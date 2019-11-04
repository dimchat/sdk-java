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
package chat.dim.protocol;

import java.util.List;
import java.util.Map;

/**
 *  Command message: {
 *      type : 0x89,
 *      sn   : 123,  // the same serial number with the original message
 *
 *      command : "mute",
 *      time    : 0,
 *      list    : []      // mute-list
 *  }
 */
public class MuteCommand extends HistoryCommand {

    public static final String MUTE   = "mute";

    // mute-list
    private List muteList = null;

    public MuteCommand(Map<String, Object> dictionary) {
        super(dictionary);
    }

    /**
     *  Update mute-list
     *
     * @param list - mute list
     */
    public MuteCommand(List list) {
        super(MUTE);
        dictionary.put("list", list);
        muteList = list;
    }

    /**
     *  Query mute-list
     */
    public MuteCommand() {
        super(MUTE);
    }

    //-------- setters/getters --------

    public List getMuteList() {
        if (muteList == null) {
            Object list = dictionary.get("list");
            if (list != null) {
                assert list instanceof List;
                muteList = (List) list;
            }
        }
        return muteList;
    }

    public void setMuteList(List list) {
        if (list == null) {
            dictionary.remove("list");
        } else {
            dictionary.put("list", list);
        }
        muteList = list;
    }
}
