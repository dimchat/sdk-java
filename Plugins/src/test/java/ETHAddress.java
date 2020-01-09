
import chat.dim.Address;
import chat.dim.protocol.NetworkType;

public final class ETHAddress extends Address {

    /**
     * Called by 'getInstance()' to create address
     *
     * @param string - Encoded address string
     */
    public ETHAddress(String string) {
        super(string);
        if (!string.substring(0, 2).equals("0x")) {
            throw new IllegalArgumentException("invalid ETH address: " + string);
        }
    }

    @Override
    public NetworkType getNetwork() {
        return null;
    }

    @Override
    public long getCode() {
        return 0;
    }


    static ETHAddress generate(byte[] fingerprint, NetworkType network) {
        // TODO: generate ETH address
        return null;
    }

    static {
        Address.register(ETHAddress.class);
    }
}
