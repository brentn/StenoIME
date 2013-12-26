package com.brentandjody.Translator;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by brent on 02/12/13.
 * Handle Plover's custom formatting cues
 */
public class Formatter {

    private static  enum CASE {Capital, Lowercase}
    private static final String punctuation = ",.!?:;";

    // state variables
    private CASE capitalization=null;
    private boolean glue=false;

    public Formatter() {
    }

    public String format (String input) {
        if (input==null || input.length()==0) return "";
        String output=input;
        String space=" ";
        boolean new_glue=false;
        CASE new_capitalization=null;
        StringBuilder sb = new StringBuilder();
        if (hasQueue() || input.contains("{")) {
            for (String atom : breakApart(input)) {
                if (atom.equals("{#Return}")) {
                    sb.append("\n"); space=""; atom="";
                }
                if (atom.equals("{#BackSpace}")) {
                    sb.append("\b"); space=""; atom="";
                }
                if (atom.equals("{#Tab}")) {
                    sb.append("\t"); space=""; atom="";
                }
                if (atom.contains("{^")) {
                    sb.append("\b"); atom = atom.replace("{^", "");
                }
                if (atom.contains("^}")) {
                    space=""; atom = atom.replace("^}", "");
                }
                // new flags
                if (atom.contains("{-|}")) {
                    new_capitalization=CASE.Capital; space=""; atom="";
                }
                if (atom.contains("{>}")) {
                    new_capitalization=CASE.Lowercase; space=""; atom="";
                }
                if (atom.contains("{&")) {
                    new_glue=true; atom = atom.replace("{&", "");
                }
                atom = atom.replace("{","").replace("}","");
                // remove space before punctuation
                if (atom.length() == 1 && punctuation.contains(atom))
                    sb.append("\b");
                sb.append(atom);
            }
            //process flags
            if (capitalization==CASE.Capital && sb.length()>0) sb.replace(0,1,sb.substring(0,1).toUpperCase());
            if (capitalization==CASE.Lowercase && sb.length()>0) sb.replace(0,1,sb.substring(0,1).toLowerCase());
            if (glue && new_glue) sb.reverse().append("\b").reverse();
            capitalization = new_capitalization;
            glue = new_glue;
            output = sb.toString();
        }
        return output+space;
    }

    public boolean hasQueue() { return (glue || (capitalization!=null)); }

    public void removeItemFromQueue() { glue=false; capitalization=null; }

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

}
