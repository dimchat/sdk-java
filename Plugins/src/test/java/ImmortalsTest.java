
import chat.dim.Immortals;
import org.junit.Test;

import chat.dim.User;
import chat.dim.ID;

public class ImmortalsTest {

    private Immortals immortals = new Immortals();
    private Facebook facebook = Facebook.getInstance();

    @Test
    public void testImmortals() {
        // Immortal Hulk
        User hulk = facebook.getUser(immortals.getID(Immortals.HULK));
        Log.info("hulk: " + hulk);

        Log.info("name: " + hulk.getName());
        Log.info("profile: " + facebook.getProfile(hulk.identifier));

        // Monkey King
        User moki = facebook.getUser(immortals.getID(Immortals.MOKI));
        Log.info("moki: " + moki);

        Log.info("name: " + moki.getName());
        Log.info("profile: " + facebook.getProfile(moki.identifier));

        // Everyone
        User anyone = new User(ID.ANYONE);
        anyone.setDataSource(facebook);
        Log.info("broadcast: " + anyone.identifier);
        Log.info("number: " + anyone.getNumber());
        Log.info("anyone: " + anyone);
        Log.info("is broadcast: " + anyone.identifier.isBroadcast());

        // Everyone
        User everyone = new User(ID.EVERYONE);
        everyone.setDataSource(facebook);
        Log.info("broadcast: " + everyone.identifier);
        Log.info("number: " + everyone.getNumber());
        Log.info("everyone: " + everyone);
        Log.info("is broadcast: " + everyone.identifier.isBroadcast());
    }
}