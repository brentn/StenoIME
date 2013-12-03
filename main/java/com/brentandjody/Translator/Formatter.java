package com.brentandjody.Translator;

import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by brent on 02/12/13.
 */
public class Formatter {

    private static  enum TYPE {Capital, Lowercase, AttachStart, AttachEnd, Glue};

    private Stack<TYPE> formatQ = new Stack<TYPE>();

    public Formatter() {
        formatQ.clear();
    }

    public String format (String input) {
        if (input==null) return "";
        if (!input.contains("/")) return input;
        return input+" ";
    }

    public boolean hasQueue() { return !formatQ.isEmpty(); }

    public void removeItemFromQueue() { formatQ.pop(); }

}
