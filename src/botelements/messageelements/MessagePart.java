package botelements.messageelements;

import java.io.Serializable;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public class MessagePart implements Serializable{
    public enum MessagePartType{
        Program, Resource, RawMessage, MentionTag}
    public MessagePartType type = MessagePartType.RawMessage;
    public String rawMessage = "";
    public ResourceMessagePart resource;
    public ProgramMessagePart program;
    public String error = "";
    public boolean isValid() {
        switch (type) {
            case RawMessage:
                return true;
            case MentionTag:
                if (rawMessage.equals("@sender") || rawMessage.equals("@bot"))
                    return true;
                error = error + "\n" + "Invalid mention tag, how the fuck did you do it?";
                return false;
            case Resource:
                if (resource != null && resource.isValid())
                    return true;
                if (resource == null)
                    error = error + "\n" + "Invalid resource, how the fuck did you do it?";
                else
                    error = error + "\n" + "There is a problem with resource: ``" + resource.name + "``";
                return false;
            case Program:
                if (program != null && program.isValid())
                    return true;
                if (program == null)
                    error = error + "\n" + "Invalid program, how the fuck did you do it?";
                else if (program.className == null)
                    error = error + "\n" + "The program class name is invalid.";
                else if (program.functionName == null)
                    error = error + "\n" + "The function name in program of class name: ``" + program.className + "``is invalid.";
                else
                    error = error + "\n" + "There is a problem with argument list of the program: ``" + program.className + "." + program.functionName + "``";
                return false;
            default:
                return false;
        }
    }
}