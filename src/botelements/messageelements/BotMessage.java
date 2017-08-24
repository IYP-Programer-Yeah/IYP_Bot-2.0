package botelements.messageelements;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public class BotMessage implements Serializable{
    public ArrayList<MessagePart> messageParts = new ArrayList<MessagePart>();
    public String message;
    public String error = "";
    public String author;
    public boolean isTestFeature = true;
    public boolean isValid() {
        boolean isValid = true;
        for (MessagePart messagePart : messageParts)
            if (!messagePart.isValid()) {
                error = error + messagePart.error;
                isValid = false;
            }
        return isValid;
    }

    public void normalizeResources() {
        for (int i = 0; i < messageParts.size(); i++)
            if (messageParts.get(i).type == MessagePart.MessagePartType.Resource && messageParts.get(i).resource.count > 0){
                for (int j = 1; j < messageParts.get(i).resource.count; j++) {
                    messageParts.add(i, messageParts.get(i));
                    i++;
                }
                messageParts.get(i).resource.count = 1;
            } else if (messageParts.get(i).type == MessagePart.MessagePartType.Program)
                messageParts.get(i).program.normalizeArguments();
    }

    public int findPreviousVariablesSizeMessagePart (int i) {
        i--;
        while (i >= 0 && messageParts.get(i).type != MessagePart.MessagePartType.Program && messageParts.get(i).type != MessagePart.MessagePartType.Resource)
            i--;
        return i;
    }
}
