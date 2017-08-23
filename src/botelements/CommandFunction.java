package botelements;

import de.btobastian.javacord.entities.message.Message;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public interface CommandFunction {
    void doFunction(Command command, Message message);
}
