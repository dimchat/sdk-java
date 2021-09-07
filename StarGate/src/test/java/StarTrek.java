
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import chat.dim.net.BaseHub;
import chat.dim.port.Gate;
import chat.dim.stargate.TCPGate;
import chat.dim.startrek.PlainArrival;
import chat.dim.startrek.PlainDeparture;
import chat.dim.startrek.PlainDocker;
import chat.dim.tcp.ClientHub;

interface StarDelegate extends Gate.Delegate<PlainDeparture, PlainArrival, Object> {
}

public final class StarTrek extends TCPGate<PlainDeparture, PlainArrival, Object> {

    public StarTrek(StarDelegate delegate) {
        super(delegate);
    }

    @Override
    protected BaseHub createHub() {
        return new ClientHub(this);
    }

    @Override
    protected PlainDocker createDocker(SocketAddress remote, SocketAddress local,
                                       List<byte[]> data) {
        return new PlainDocker(remote, local, data, this);
    }

    public void start() {
        (new Thread(this)).start();
    }

    public static StarTrek create(String host, int port, StarDelegate delegate) throws IOException {
        StarTrek gate = new StarTrek(delegate);
        gate.connect(new InetSocketAddress(host, port), null);
        return gate;
    }
}
