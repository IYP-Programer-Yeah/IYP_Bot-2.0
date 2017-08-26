package botelements.messageelements;

import botelements.Bot;
import javafx.util.Pair;

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
    public Pair<Boolean, Integer> evaluateArgumentResourceIndices(ArrayList<Pair<Integer, ResourceMessagePart>> availableResources, boolean[] isInUse) {
        boolean isPerfect = true;
        int danglingResourceCount = 0;
        for (ResourceMessagePart argument : arguments) {
                Pair<Boolean, Boolean> resourceIndex = argument.evaluateResourceIndex(availableResources, isInUse);
                if (!resourceIndex.getValue())
                    danglingResourceCount++;
                isPerfect = isPerfect && resourceIndex.getKey();
            }
        return new Pair<>(isPerfect, danglingResourceCount);
    }
    public boolean checkForwardReferencing(int i) {
        for (ResourceMessagePart argument : arguments)
            if (argument.resourceIndex>i)
                return true;
        return false;
    }
}
