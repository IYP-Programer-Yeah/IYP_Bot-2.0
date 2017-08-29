package utility;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by HoseinGhahremanzadeh on 8/21/2017.
 */
public class NFA {
    private HashMap<Character, LinkedList<NFA>> children = new HashMap<>();
    private boolean accepts = false;
    public void put(String token) {
        LinkedList<NFA> dsts = new LinkedList<NFA>();
        LinkedList<NFA> nextDsts = new LinkedList<NFA>();
        dsts.add(this);

        for (int i=0; i<token.length(); i++)
            for (NFA dst : dsts){
                if (!dst.children.containsKey(token.charAt(i))) {
                    dst.children.put(token.charAt(i), new LinkedList<NFA>());
                    dst.children.get(token.charAt(i)).add(new NFA());
                }
                if (i == (token.length() - 1) && !dst.children.get(token.charAt(i)).contains(this))
                    dst.children.get(token.charAt(i)).add(this);
                nextDsts.addAll(dst.children.get(token.charAt(i)));
                dsts = nextDsts;
            }
        for (NFA dst : dsts)
            dst.accepts = true;
    }
    public LinkedList<NFA> process(char key, LinkedList<NFA> currentStates) {
        LinkedList<NFA> result = new LinkedList<NFA>();
        for (NFA currentNFA : currentStates)
            if (currentNFA.children.get(key) != null)
                result.addAll(currentNFA.children.get(key));
        return result;
    }
    public boolean accepts(LinkedList<NFA> currentStates) {
        for (NFA currentNFA : currentStates)
            if (currentNFA.accepts)
                return true;
        return false;
    }
}
