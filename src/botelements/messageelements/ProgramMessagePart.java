package botelements.messageelements;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by HoseinGhahremanzadeh on 8/21/2017.
 */
public class ProgramMessagePart implements Serializable{
    public String className;
    public String functionName;
    public ArrayList<ResourceMessagePart> arguments = new ArrayList<>();
    public boolean isValid() {
        for (ResourceMessagePart currentArgument : arguments)
            if (!currentArgument.isValid())
                return false;
        return className != null && functionName != null && className.length() > 0 && functionName.length() > 0;
    }
    public void normalizeArguments() {
        for (int i = 0; i < arguments.size(); i++)
            if (arguments.get(i).count > 0){
                for (int j = 1; j < arguments.get(i).count; j++) {
                    arguments.add(i, arguments.get(i));
                    i++;
                }
                arguments.get(i).count = 1;
            }
    }
}
