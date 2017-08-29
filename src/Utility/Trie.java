package utility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by HoseinGhahremanzadeh on 8/4/2017.
 */
public class Trie implements Serializable {
    private HashMap<Character, Trie> children = new HashMap<>();
    private Object object = null;
    private ArrayList<String> keys = new ArrayList<String>();
    public Object put(String key, Object object) {
        Trie dst = this;
        for (int i=0; i<key.length(); i++) {
            if (!dst.children.containsKey(key.charAt(i)))
                dst.children.put(key.charAt(i), new Trie());
            dst = dst.children.get(key.charAt(i));
        }
        Object temp = dst.object;
        dst.object = object;
        keys.add(key);
        return temp;
    }
    public Object remove(String key) {
        Trie dst = this;
        Trie emptyBranchParent = this;
        Character emptyBranchKey = key.charAt(0);
        for (int i=0; i<key.length(); i++) {
            if (!dst.children.containsKey(key.charAt(i)))
                return null;
            if (dst.children.size() > 1 || dst.object != null) {
                emptyBranchParent = dst;
                emptyBranchKey = key.charAt(i);
            }
                dst = dst.children.get(key.charAt(i));
        }
        Object temp = dst.object;
        dst.object = object;
        if (dst.children.size() > 0)
            dst.object = null;
        else
            emptyBranchParent.children.remove(emptyBranchKey);
        keys.remove(key);
        return temp;
    }
    public Trie process(char key) {
        return children.get(key);
    }

    public ArrayList<String> getKeys() {
        return keys;
    }
    public boolean accepts() {
        return object != null;
    }
    public String getRandomKey() {
        Random rnd = new Random(System.currentTimeMillis());
        return keys.get(Math.abs(rnd.nextInt()%keys.size()));
    }
}
