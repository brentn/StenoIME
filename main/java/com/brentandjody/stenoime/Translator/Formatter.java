package com.brentandjody.stenoime.Translator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by brent on 02/12/13.
 * Handle Plover's custom formatting cues
 */
public class Formatter {

    public static enum CASE {Capital, Lowercase}
    public static enum ACTION {Update_State, Ignore_state}
    public static final String punctuation = ":;,";

    private int mBackspaces;
    private boolean mSuffix;
    // state variables
    private CASE mCapitalization =null;
    private boolean mGlue=false;
    private int mPriorSpaces=0;

    public Formatter() {
    }

    public String format (String input) {
        return format (input, ACTION.Update_State);  //format string and update state by default
    }

    public String format (String input, ACTION action) {
        State prior_state = getState();
        mSuffix = false;
        mBackspaces =0;
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
                mSuffix = isSuffix(atom);
                if (atom.equals("{#Return}")) {
                    sb.append(backspaces(mPriorSpaces)+"\n"); space=""; atom="";mPriorSpaces=0;
                }
                if (atom.equals("{#BackSpace}")) {
                    sb.append("\b"); space=""; atom="";
                }
                if (atom.equals("{#Tab}")) {
                    sb.append(backspaces(mPriorSpaces)+"\t"); space=""; atom="";mPriorSpaces=0;
                }
                if (atom.equals("{,}")) {
                    sb.append(backspaces(mPriorSpaces)+","); atom="";mPriorSpaces=0;
                }
                if (atom.equals("{.}")) {
                    sb.append(backspaces(mPriorSpaces)+"."); new_capitalization=CASE.Capital; atom="";mPriorSpaces=0;
                }
                if (atom.equals("{?}")) {
                    sb.append(backspaces(mPriorSpaces)+"?"); new_capitalization=CASE.Capital; atom="";mPriorSpaces=0;
                }
                if (atom.equals("{!}")) {
                    sb.append(backspaces(mPriorSpaces)+"!"); new_capitalization=CASE.Capital; atom="";mPriorSpaces=0;
                }
                if (atom.contains("{^")) {
                    if (sb.length()==0 || sb.charAt(sb.length()-1) == ' ') {
                        sb.append(backspaces(mPriorSpaces));mPriorSpaces=0;
                    }
                    atom = atom.replace("{^", "");
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
                if (atom.length() == 1 && punctuation.contains(atom)) {
                    sb.append(backspaces(mPriorSpaces));
                    mPriorSpaces = 0;
                }
                sb.append(atom);
            }
            // process flags
            boolean text_has_letters = sb.toString().matches(".*[a-zA-Z]+.*");
            if (mGlue && new_glue) {
                sb.reverse().append(backspaces(mPriorSpaces)).reverse();
                mPriorSpaces = 0;
            }
            if (text_has_letters) {
                if (mCapitalization ==CASE.Capital)
                    sb.replace(0,1,sb.substring(0,1).toUpperCase());
                if (mCapitalization ==CASE.Lowercase)
                    sb.replace(0,1,sb.substring(0,1).toLowerCase());
            }
            mGlue = new_glue;
            if (text_has_letters || new_capitalization!=null) {
                mCapitalization = new_capitalization;
            }

            output = sb.toString();
        }
        mPriorSpaces=space.length();
        if (action==ACTION.Ignore_state) {
            restoreState(prior_state);
        }
        if (output.length()>0 && output.charAt(output.length()-1)==' ' && space==" ") //if output already ends in a space, don't add another
            space="";
        return remove_backspaces(output)+space;
    }

    public State getState() {
        return new State(mCapitalization, mGlue, mSuffix, mPriorSpaces);
    }

    public void resetState() {
        mCapitalization =null;
        mGlue =false;
        mSuffix =false;
        mPriorSpaces=0;
    }

    public void restoreState(State state) {
        mCapitalization = state.getCapitalization();
        mGlue = state.hasGlue();
        mSuffix = state.isSuffix();
        mPriorSpaces = state.getmPriorSpaces();
    }

    public boolean hasFlags() { return (mGlue || (mCapitalization !=null)); }

    public int backspaces() {return mBackspaces;}


    public void removeItemFromQueue() { mGlue =false; mCapitalization =null; }

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

    public boolean wasSuffix() { return mSuffix; }

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

    private static String backspaces(int length) {
        char[] result = new char[length];
        Arrays.fill(result, '\b');
        return new String(result);
    }

    private String remove_backspaces(String text) {
        StringBuilder result = new StringBuilder();
        boolean start = true;
        int i=0;
        for (char c : text.toCharArray()) {
            if (c=='\b') {
                if (start) {
                    mBackspaces++;
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
        private CASE mCapitalization;
        private boolean mGlue;
        private boolean mSuffix;
        private int mPriorSpaces;

        public State(CASE capitalization, boolean glue, boolean suffix, int prior_spaces) {
            this.mCapitalization=capitalization;
            this.mGlue=glue;
            this.mSuffix=suffix;
            this.mPriorSpaces=prior_spaces;
        }

        public CASE getCapitalization() {
            return mCapitalization;
        }
        public boolean hasGlue() {
            return mGlue;
        }
        public boolean isSuffix() {
            return mSuffix;
        }
        public int getmPriorSpaces() {return mPriorSpaces;}
    }

}
