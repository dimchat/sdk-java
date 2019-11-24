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

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Messenger;
import chat.dim.protocol.Command;
import chat.dim.protocol.TextContent;

public class HistoryCommandProcessor extends CommandProcessor {

    private GroupCommandProcessor gpu = null;

    public HistoryCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private GroupCommandProcessor getGPU() {
        if (gpu == null) {
            gpu = (GroupCommandProcessor) createProcessor(GroupCommandProcessor.class);
        }
        return gpu;
    }

    //-------- Main --------

    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof Command;
        assert getClass() == HistoryCommandProcessor.class; // override me!

        CommandProcessor cpu;
        if (content.getGroup() == null) {
            // process command content by name
            String command = ((Command) content).command;
            cpu = getCPU(command);
            if (cpu == null) {
                String text = String.format("History command (%s) not support yet!", command);
                return new TextContent(text);
            }
        } else {
            // call group command processor
            cpu = getGPU();
        }
        assert cpu != this; // Dead cycle!
        return cpu.process(content, sender, iMsg);
    }
}
