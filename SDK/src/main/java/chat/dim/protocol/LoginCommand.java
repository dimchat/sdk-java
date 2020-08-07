/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
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
import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.network.ServiceProvider;
import chat.dim.network.Station;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "login",
 *      time    : 0,
 *      //---- client info ----
 *      ID       : "{UserID}",
 *      device   : "DeviceID",  // (optional)
 *      agent    : "UserAgent", // (optional)
 *      //---- server info ----
 *      station  : {
 *          ID   : "{StationID}",
 *          host : "{IP}",
 *          port : 9394
 *      },
 *      provider : {
 *          ID   : "{SP_ID}"
 *      }
 *  }
 */
public class LoginCommand extends Command {

    public final Date time;

    public LoginCommand(Map<String, Object> dictionary) {
        super(dictionary);
        Object timestamp = dictionary.get("time");
        if (timestamp == null) {
            time = null;
        } else {
            time = getDate((Number) timestamp);
        }
    }

    public LoginCommand(ID identifier) {
        super(LOGIN);
        put("ID", identifier);
        time = new Date();
        put("time", getTimestamp(time));
    }

    private long getTimestamp(Date time) {
        return time.getTime() / 1000;
    }
    private Date getDate(Number timestamp) {
        return new Date(timestamp.longValue() * 1000);
    }

    //
    //  Client Info
    //

    // user ID
    public Object getIdentifier() {
        return get("ID");
    }

    // device ID
    public String getDevice() {
        return (String) get("device");
    }
    public void setDevice(String device) {
        put("device", device);
    }

    // user-agent
    public String getAgent() {
        return (String) get("agent");
    }
    public void setAgent(String agent) {
        put("agent", agent);
    }

    //
    //  Server Info
    //

    // station
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStation() {
        return (Map<String, Object>) get("station");
    }
    public void setStation(Map station) {
        put("station", station);
    }
    public void setStation(Station station) {
        Map<String, Object> info = new HashMap<>();
        info.put("ID", station.identifier);
        info.put("host", station.getHost());
        info.put("port", station.getPort());
        put("station", info);
    }

    // service provider
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProvider() {
        return (Map<String, Object>) get("provider");
    }
    public void setProvider(Map provider) {
        put("provider", provider);
    }
    public void setProvider(ServiceProvider provider) {
        Map<String, Object> info = new HashMap<>();
        info.put("ID", provider.identifier);
        put("provider", info);
    }
}
