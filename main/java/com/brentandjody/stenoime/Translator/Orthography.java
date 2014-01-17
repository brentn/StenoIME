package com.brentandjody.stenoime.Translator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brentn on 16/01/14.
 */
public class Orthography {

    private static final Rule RULE1 = new Rule(Pattern.compile("^(.*[aeiou]c) \\^ ly$", Pattern.CASE_INSENSITIVE), "\\1ally");
    private static final Rule RULE2 = new Rule(Pattern.compile("^(.*(?:s|sh|x|z|zh)) \\^ s$", Pattern.CASE_INSENSITIVE), "\\1es");
    private static final Rule RULE3 = new Rule(Pattern.compile("^(.*(?:oa|ea|i|ee|oo|au|ou|l|n|(?<![gin]a)r|t)ch) \\^ s$", Pattern.CASE_INSENSITIVE), "\\1es");
    private static final Rule RULE4 = new Rule(Pattern.compile("^(.+[bcdfghjklmnpqrstvwxz])y \\^ s$", Pattern.CASE_INSENSITIVE), "\\1ies");
    private static final Rule RULE5 = new Rule(Pattern.compile("^(.+)ie \\^ ing$$", Pattern.CASE_INSENSITIVE), "\\1ying");
    private static final Rule RULE6 = new Rule(Pattern.compile("^(.+[cdfghlmnpr])y \\^ ist$", Pattern.CASE_INSENSITIVE), "\\1ist");
    private static final Rule RULE7 = new Rule(Pattern.compile("^(.+[bcdfghjklmnpqrstvwxz])y \\^ ([a-hj-xz].*)", Pattern.CASE_INSENSITIVE), "\\1i\\2");
    private static final Rule RULE8 = new Rule(Pattern.compile("^(.+)([t])e \\^ en$", Pattern.CASE_INSENSITIVE), "\\1\\2\\2en");
    private static final Rule RULE9 = new Rule(Pattern.compile("^(.+[bcdfghjklmnpqrstuvwxz])e \\^ ([aeiouy].*)$", Pattern.CASE_INSENSITIVE), "\\1\\2");
    private static final Rule RULE10 = new Rule(Pattern.compile("^(.*(?:[bcdfghjklmnprstvwxyz]|qu)[aeiou])([bcdfgklmnprtvz]) \\^ ([aeiouy].*)$", Pattern.CASE_INSENSITIVE), "\\1\\2\\2\\3");

    private static final Rule[] RULES = new Rule[]{RULE1, RULE2, RULE3, RULE4, RULE5, RULE6, RULE7, RULE8, RULE9, RULE10};

    public String match(String word, String suffix) {
        List<String> candidates = new ArrayList<String>();
        Matcher matcher;
        String result;
        for (Rule rule : RULES) {
            matcher = rule.pattern().matcher(word + " ^ " + suffix);
            if (matcher.find()) {
                result = rule.replace();
                if (matcher.groupCount() > 0) result=result.replace("\\1", matcher.group(1));
                if (matcher.groupCount() > 1) result=result.replace("\\2", matcher.group(2));
                if (matcher.groupCount() > 2) result=result.replace("\\3", matcher.group(3));
                candidates.add(result);
            }
        }
        if (candidates.isEmpty()) return "";
        return candidates.get(0);
    }

    private static class Rule {
        private Pattern pattern;
        private String replace;

        public Rule(Pattern p, String s) {
            pattern = p;
            replace = s;
        }

        public Pattern pattern() { return pattern; }
        public String replace() {return replace; }
    }
}
