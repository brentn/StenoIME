package com.brentandjody.stenoime.Translator;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by brent on 02/12/13.
 * Handle Plover's custom formatting cues
 */
public class Formatter {

    public static  enum CASE {Capital, Lowercase}
    public static final String punctuation = ":;,";

    private int backspaces;
    private boolean suffix;
    // state variables
    private CASE capitalization=null;
    private boolean glue=false;
    private boolean trim=false;

    public Formatter() {
    }

    public String format (String input) {
        return format (input, false);  //format string and update state by default
    }

    public String format (String input, boolean reset_state) {
        suffix = false;
        State prior_state = getState();
        backspaces=0;
        trim=false;
        if (input==null || input.length()==0) return "";
        if (isNumeric(input.replace("/",""))) {
            input = "{&"+input.replace("/","")+"}";
        }
        String output=input;
        String space = " ";
        boolean new_glue=false;
        CASE new_capitalization=null;
        StringBuilder sb = new StringBuilder();
        if (hasFlags() || input.contains("{")) {
            for (String atom : breakApart(input)) {
                suffix = isSuffix(atom);
                if (atom.equals("{#Return}")) {
                    sb.append("\n"); space=""; atom=""; trim=true;
                }
                if (atom.equals("{#BackSpace}")) {
                    sb.append("\b"); space=""; atom="";
                }
                if (atom.equals("{#Tab}")) {
                    sb.append("\t"); space=""; atom=""; trim=true;
                }
                if (atom.equals("{,}")) {
                    sb.append(","); atom=""; trim=true;
                }
                if (atom.equals("{.}")) {
                    sb.append("."); new_capitalization=CASE.Capital; atom=""; trim=true;
                }
                if (atom.equals("{?}")) {
                    sb.append("?"); new_capitalization=CASE.Capital; atom=""; trim=true;
                }
                if (atom.equals("{!}")) {
                    sb.append("!"); new_capitalization=CASE.Capital; atom=""; trim=true;
                }
                if (atom.contains("{^")) {
                    if (sb.length()==0 || sb.charAt(sb.length()-1) == ' ') {
                        trim=true;
                    }
                    atom = atom.replace("{^", "");
                }
                if (atom.contains("^}")) {
                    space=""; atom = atom.replace("^}", "");
                }
                // new flags
                if (atom.contains("{-|}")) {
                    new_capitalization=CASE.Capital; space=""; atom=""; trim=true;
                }
                if (atom.contains("{>}")) {
                    new_capitalization=CASE.Lowercase; space=""; atom=""; trim=true;
                }
                if (atom.contains("{&")) {
                    new_glue=true; atom = atom.replace("{&", "");
                }
                atom = atom.replace("{","").replace("}","");
                // remove space before punctuation
                if (atom.length() == 1 && punctuation.contains(atom))
                    trim=true;
                sb.append(atom);
            }
            // process flags
            boolean text_not_empty = !sb.toString().replace("\n","").isEmpty();
            if (glue && new_glue)
                trim=true;
                //sb.reverse().append("\b").reverse();
            if (text_not_empty) {
                if (capitalization==CASE.Capital)
                    sb.replace(0,1,sb.substring(0,1).toUpperCase());
                if (capitalization==CASE.Lowercase)
                    sb.replace(0,1,sb.substring(0,1).toLowerCase());
            }
            glue = new_glue;
            if (text_not_empty || new_capitalization!=null) {
                capitalization = new_capitalization;
            }

            output = sb.toString();
            if (reset_state) {
                restoreState(prior_state);
            }
        }
        return remove_backspaces(output)+space;
    }

    public State getState() {
        return new State(capitalization, glue, suffix, trim);
    }

    public void resetState() {
        capitalization=null;
        glue=false;
        suffix=false;
        trim=false;
    }

    public void restoreState(State state) {
        capitalization = state.getCapitalization();
        glue = state.hasGlue();
        suffix = state.suffix();
        trim = state.trim();
    }

    public boolean hasFlags() { return (glue || (capitalization!=null)); } //should this return true if trim is set?

    public int backspaces() {return backspaces;}

    public boolean trim() {return trim;}

    public void removeItemFromQueue() { glue=false; capitalization=null; trim=false;}

    public List<String> breakApart(String s) {
        //break a translation string into atoms. (recursive)
        List<String> result = new LinkedList<String>();
        if (s == null || s.isEmpty()) return result;
        if ((s.contains("{") && s.contains("}"))) {
            int start = s.indexOf("{");
            if (start==0) { //first atom is {}
                int end = s.indexOf("}")+1; //substring is (] (exclusive at end)
                result.add(s.substring(start,end));
                if (end < s.length()) {
                    result.addAll(breakApart(s.substring(end)));
                }
            } else { // add text prior to {
                result.add(s.substring(0, start));
                result.addAll(breakApart(s.substring(start)));
            }
        } else {
            result.add(s);
        }
        return result;
    }

    public boolean wasSuffix() { return suffix; }

    private boolean isSuffix(String atom) {
        if (atom==null) return false;
        if (atom.length()<3) return false;
        if (atom.charAt(atom.length()-2)=='^') return false; //it is a joiner, not a suffix
        return atom.charAt(0) == '{' && atom.charAt(1) == '^';
    }

    public static boolean isNumeric(String s) {
        if (s==null || s.length()==0) return false;
        for (char c : s.toCharArray()) {
            if ("0123456789".indexOf(c)==-1) return false;
        }
        return true;
    }

    private String remove_backspaces(String text) {
        StringBuilder result = new StringBuilder();
        boolean start = true;
        int i=0;
        for (char c : text.toCharArray()) {
            if (c=='\b') {
                if (start) {
                    backspaces++;
                } else {
                    result.deleteCharAt(i-1);
                    i--;
                }
            } else {
                start = false;
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    public class State {
        private CASE capitalization;
        private boolean glue;
        private boolean suffix;
        private boolean trim; //trim spaces from end of current output

        public State(CASE capitalization, boolean glue, boolean suffix, boolean trim) {
            this.capitalization=capitalization;
            this.glue=glue;
            this.suffix=suffix;
            this.trim=trim;
        }

        public CASE getCapitalization() {
            return capitalization;
        }
        public boolean hasGlue() {
            return glue;
        }
        public boolean suffix() {
            return suffix;
        }
        public boolean trim() {return trim;}
    }

}
