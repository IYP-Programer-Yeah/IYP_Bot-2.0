package botelements.messageelements;

import java.io.Serializable;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public class ResourceMessagePart implements Serializable{
    public String name = "";
    public int id = -1;
    public int count = 1;
    public boolean isValid() {
        return name.length() > 0 && count > -3;
    }

    @Override
    public boolean equals(Object obj) {
        ResourceMessagePart inp = (ResourceMessagePart)obj;
        return name.equals(inp.name) && id == inp.id && count == inp.count;
    }
}
