/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim;

import java.util.Map;

import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.StorageCommand;

public class CommandParser extends Command.Parser {

    @Override
    protected Command parseCommand(Map<String, Object> cmd, String name) {
        // parse core command first
        Command core = super.parseCommand(cmd, name);
        if (core != null) {
            return core;
        }

        if (Command.RECEIPT.equals(name)) {
            return new ReceiptCommand(cmd);
        }
        if (Command.HANDSHAKE.equals(name)) {
            return new HandshakeCommand(cmd);
        }
        if (Command.LOGIN.equals(name)) {
            return new LoginCommand(cmd);
        }

        if (MuteCommand.MUTE.equals(name)) {
            return new MuteCommand(cmd);
        }
        if (BlockCommand.BLOCK.equals(name)) {
            return new BlockCommand(cmd);
        }

        // storage (contacts, private_key)
        if (StorageCommand.STORAGE.equals(name)) {
            return new StorageCommand(cmd);
        }
        if (StorageCommand.CONTACTS.equals(name)) {
            return new StorageCommand(cmd);
        }
        if (StorageCommand.PRIVATE_KEY.equals(name)) {
            return new StorageCommand(cmd);
        }

        return null;
    }
}
