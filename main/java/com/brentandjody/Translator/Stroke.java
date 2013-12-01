package com.brentandjody.Translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Stoke object - represents a single steno stroke (ie, no "/")
 * represents a sequence of steno keys
 * Accepts only keys in STENO_KEYS and rejects everything else
 */
public class Stroke {

    public static final String SEND_STROKE = "send stroke";

    private static final Set<String> IMPLICIT_HYPHENS = new HashSet<String>() {{
        add("A-"); add("O-"); add("5-"); add("0-"); add("-E"); add("-U"); add("*");
    }};
    private static final HashMap<String,String> NUMBER_KEYS = new LinkedHashMap<String, String>() {{
        put("S-", "1-"); put("T-", "2-"); put("P-", "3-"); put("H-", "4-"); put("A-", "5-");
        put("O-", "0-"); put("-F", "-6"); put("-P", "-7"); put("-L", "-8"); put("-T", "-9");
    }};
    private static final List<String> STENO_KEYS = new LinkedList<String>(Arrays.asList("#", "S-","T-","K-",
            "P-","W-","H-","R-","A-","O-","*","-E","-U","-F","-R","-P","-B","-L","-G","-T","-S","-D","-Z"));

    public static String combine(Stroke[] strokes) {
        if (strokes == null || strokes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Stroke s : strokes) {
            sb.append(s.rtfcre());
            sb.append('/');
        }
        if (sb.charAt(sb.length()-1) == '/') {
            sb.deleteCharAt((sb.length()-1));
        }
        return sb.toString();
    }

    public static Stroke[] separate(String outline) {
        if (outline == null || outline.isEmpty()) return null;
        List<Stroke> list = new LinkedList<Stroke>();
        for (String s : outline.split("/")) {
            list.add(new Stroke(s));
        }
        Stroke[] result = new Stroke[list.size()];
        return list.toArray(result);
    }


    public static Set<String> normalize(String input) {
        List<String> keys = new ArrayList<String>();
        boolean single_hyphen = (input.contains("-") && (input.indexOf("-") == input.lastIndexOf("-")));
        boolean rightSide = false;
        for (int i=0; i<input.length(); i++) {
            switch (input.charAt(i)) {
                case '#':keys.add("#");
                    break;
                case '*':keys.add("*");
                    break;
                case '-':
                    if (single_hyphen) rightSide=true; //if there is only 1 hyphen, use it to distinguish right/left
                    if (i < (input.length()-1)) {
                        rightSide=true;
                        keys.add("-"+input.charAt(i+1));
                        i++;
                    } // otherwize it is a spurious trailing hyphen, ignore it
                    break;
                case 'E':case 'U':case 'F':case 'B':case 'L':case 'G':case 'D':case 'Z':
                    //keys that only exist on the right side
                    rightSide=true;
                    keys.add("-"+input.charAt(i));
                    break;
                case 'A':case 'O':
                    rightSide=true;
                case 'K':case 'W':case 'H':
                    //keys that only exist on the left side
                    keys.add(input.charAt(i)+"-");
                    if ((i < (input.length()-1)) && (input.charAt(i+1) == '-') && (!single_hyphen))
                        i++;
                    break;
                default:
                    // these keys exist on both sides, or are invalid
                    if ("STPR".indexOf(input.charAt(i))>=0) {
                        if ((i < (input.length()-1)) && (input.charAt(i+1) == '-')) { //is the next char a hyphen?
                            keys.add(input.charAt(i)+"-");
                            if (!single_hyphen) i++;
                            break;
                        }
                        if (rightSide || keys.contains(input.charAt(i)+"-")) { //if we have already had characters on the right, or if we alredy have this letter on the left, assume right side
                            keys.add("-"+input.charAt(i));
                            break;
                        }
                        // else assume left side
                        keys.add(input.charAt(i)+"-");
                    } // else invalid key
            }
        }
        return new LinkedHashSet<String>(keys);
    }

    private String raw;
    private Set<String> keys;
    private String rtfcre;
    private final boolean isCorrection;

    public Stroke(Set<String> keys) {
        //sort and remove invalid and duplicate keys
        this.keys = keys;
        raw = "";
        for (String k : keys) {
            raw += k;
        }
        List<String> stroke_keys= new LinkedList<String>();
        for (String key : STENO_KEYS) {
            if (keys.contains(key)) {
                stroke_keys.add(key);
            }
        }
        rtfcre = constructStroke(convertNumbers(stroke_keys));
        isCorrection = (rtfcre.equals("*"));
    }

    public Stroke(String keyString) {
        raw=keyString;
        keyString = keyString.split("/")[0];
        keys = normalize(keyString);
        rtfcre = constructStroke(convertNumbers(keys));
        isCorrection = (rtfcre.equals("*"));
    }

    public String rtfcre() {
        return rtfcre;
    }

    public Set<String> keys() {
        return keys;
    }

    public void removeKey(String k) {
        keys.remove(k);
        rtfcre = constructStroke(convertNumbers(keys));
    }

    public Stroke[] asArray() {
        Stroke[] result = new Stroke[1];
        result[0] = this;
        return result;
    }

    public boolean isCorrection() {
        return isCorrection;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Stroke && this.rtfcre.equals(((Stroke) o).rtfcre());
    }

    @Override
    public String toString() {
        String result = "Stroke(";
        if (isCorrection) result = "*Stroke(";
        result += rtfcre + " : " + raw + ")";
        return result;
    }

    private List<String> convertNumbers(Collection<String> keys) {
        // convert appropriate letters to numbers if the rtfcre contains '#'
        if ((keys==null) || (!keys.contains("#"))) return new LinkedList<String>(keys);
        List<String> result = new LinkedList<String>();
        boolean numeral = false;
        for (String key : keys) {
            if (NUMBER_KEYS.containsKey(key)) {
                result.add(NUMBER_KEYS.get(key));
                numeral = true;
            } else {
                result.add(key);
            }
        }
        if (numeral) {
            result.remove("#");
        }
        return result;
    }

    private String constructStroke(List<String> input) {
        if (input==null) return "";
        //sort according to steno order
        List<String> chord = new LinkedList<String>();
        for (String key : STENO_KEYS)  if (input.contains(key)) chord.add(key);
        boolean number=false;
        for (String key : NUMBER_KEYS.values()) if (input.contains(key)) { number=true; chord.add(key); }
        String result = "";
        String suffix = "";
        if (! Collections.disjoint(chord, IMPLICIT_HYPHENS)) {
            for (String key : chord) {
                result += key.replace("-","");
            }
        } else {
            for (String key : chord) {
                if (number || key.charAt(key.length()-1) == '-') {
                    result += key.replace("-", "");
                } else {
                    if (key.charAt(0) == '-') {
                        suffix += key.replace("-", "");
                    }
                }
            }
            if (! suffix.isEmpty()) {
                result += "-"+suffix;
            }
        }
        return result;
    }
}
