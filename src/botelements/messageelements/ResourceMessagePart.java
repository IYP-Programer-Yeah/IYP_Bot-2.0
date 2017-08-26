package botelements.messageelements;

import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by HoseinGhahremanzadeh on 7/11/2017.
 */
public class ResourceMessagePart implements Serializable{
    public String name = "";
    public int id = -1;
    public int count = 1;
    public boolean isCaseSensitive = false;
    public int resourceIndex = -1;

    public boolean isValid() {
        return name.length() > 0 && count > -3;
    }

    @Override
    public boolean equals(Object obj) {
        ResourceMessagePart inp = (ResourceMessagePart)obj;
        return name.equals(inp.name) && id == inp.id && count == inp.count;
    }

    public Pair<Boolean, Boolean> evaluateResourceIndex(ArrayList<Pair<Integer, ResourceMessagePart>> availableResources, boolean[] isInUse) {
        if (id == -1)
            return new Pair<>(true, true);

        int i = 0;
        for (Pair<Integer, ResourceMessagePart> messageResource : availableResources) {
            if (equals(messageResource.getValue())) {
                resourceIndex = messageResource.getKey();
                if (!isInUse[i]) {
                    isInUse[i] = true;
                    break;
                }
            }
            i++;
        }
        return new Pair<Boolean, Boolean> (resourceIndex != -1 && i != availableResources.size() ,resourceIndex != -1);
    }
}
