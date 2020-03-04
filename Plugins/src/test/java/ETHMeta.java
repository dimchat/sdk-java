
import java.util.Map;

import chat.dim.Address;
import chat.dim.Meta;
import chat.dim.protocol.MetaType;

public class ETHMeta extends Meta {

    public ETHMeta(Map<String, Object> dictionary) {
        super(dictionary);
    }

    protected Address generateAddress(byte network) {
        if ((getVersion() & MetaType.ETH.value) != MetaType.ETH.value) {
            throw new ArithmeticException("meta version error");
        }
        // BTC, ExBTC
        return ETHAddress.generate(getKey().getData(), network);
    }

    static {
        Meta.register(MetaType.ETH, ETHMeta.class);
        Meta.register(MetaType.ExETH, ETHMeta.class);
    }
}
