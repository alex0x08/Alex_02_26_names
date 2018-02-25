package com.x0x08.processing.phonetic;

import info.debatty.java.stringsimilarity.JaroWinkler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ASR post-processor. A simplest one.
 * Uses Metaphone3, implementation was taken from here:
 * https://github.com/OpenRefine/OpenRefine/blob/d5fb07384242d07241b0ce103c47d930046f1135/main/src/com/google/refine/clustering/binning/Metaphone3.java
 * and Levenshtein
 * from here: https://github.com/tdebatty/java-string-similarity#levenshtein
 * 
 * There are some general limitations:
 * 1) this post-processor strictly relies on order of context records:
 * 
 * if context has for example:
 * 
 * 1."Tom Hanks" 
 * 2."Tom Cruise" 
 * 3."Clint Eastwood"
 * 
 * then each sentence will be searched in input string in exact order: 
 * 
 * tomorrow I have a meeting with 1.(Tim Hanks) 2.(Tom Crus) and .3(Eastwud)
 * 
 * so 'Tim Hanks' after 'Tom Crus' will not be found
 * 
 * 2) it's not optimized for lot of records: both input string and context should be small,
 * will work slow on even 10k+ records in context.
 * 
 * 
 * @author alex
 */
public class PostProcessor {

    private List<ContextSentence> parts = new ArrayList<>();

    private final Object lock = new Object();

    private static final Logger LOG = Logger.getLogger(PostProcessor.class.getName());

    
    /**
     * setup initial context
     *
     * @param context list of sentenses like: John Wayne Tom Hanks Tom Cruise
     * Clint Eastwood Jon Hamm John Nolan William Fitcher
     */
    public void setContext(final List<String> context) {

        Objects.requireNonNull(context, "context should exist!");

        List<ContextSentence> pparts = new ArrayList<>();

        Metaphone3 m3 = new Metaphone3();
        m3.SetEncodeVowels(true);
	m3.SetEncodeExact(true);

        context.forEach((s) -> {

            List<Word> words = new ArrayList<>();

            for (String t : cleanSplit(s)) {
                // calculate and store Metaphone index for each word of every 
                // sentence in context
                m3.SetWord(t); m3.Encode();
                words.add(new Word(t, m3.GetMetaph()));
            }

            pparts.add(new ContextSentence(s, words));
        });

        // a way to setup context safely
        synchronized (lock) {
            this.parts = Collections.unmodifiableList(pparts);
        }

    }

    /**
     * Main processing function. Thread-safe.
     *
     * @param text input sentence, ex: Jonn invited me Jon Ham and Jon Wane,
     * over for a lunch
     * @return map with possible corrections, ex: Willam | William 
     *                                              Crus | Cruise
     */
    public Map<String, String> correctNames(String text) {

        Objects.requireNonNull(text, "input text should exist!");

        String[] tpls = Arrays.stream(cleanSplit(text))
                .filter(s -> !s.isEmpty() && s.length() > 2) // drop everything 
                .toArray(String[]::new);                    // smaller than 2 chars

        LOG.log( Level.INFO, "input text cleaned: {0} ", String.join(" ", tpls) );
        
        PrContext ctx = new PrContext();

        main_loop: // for each word in input sentence
        for (int i = 0; i < tpls.length; i++) {

            String tw = tpls[i]; // current word in input sentence

            for (ContextSentence cs : parts) {  // for each sentence in context

                for (Word w : cs.words) { // for each word in sentence

                    if (!matchW(ctx, w, tw)) {
                        continue; // no first match, 
                    }            // continue to next word

                    ctx.addResult(tw, w.text); // if was first match

                    // try to match whole 
                    //sentence (in context)
                    // skip number of words in current context sentence
                    if (matchSentense(ctx, cs, tpls, i)) {
                        i += cs.words.size() - 1;continue main_loop;
                    }
                    

                }

            }

        }
        return Collections.unmodifiableMap(ctx.results);
    }

    /**
     * Will obliviously clean input string and split into words by space
     *
     * @param text some sentence
     * @return array of words
     */
    private String[] cleanSplit(String text) {
        return text.replaceAll("[^a-zA-Z0-9 ]+", "").split(" ");
    }

    /**
     * match single word, uses Jaro Winkler and Metaphone3
     *
     * @param ctx processing context
     * @param w a word
     * @param tw word to match ( in input sentence )
     * @return true if matches, false otherwise
     */
    private boolean matchW(PrContext ctx, Word w, String tw) {

        double si = ctx.jw.similarity(w.text, tw);

        if (si > 0.8) { // 0.8 means 'something like', 1.0 = full match
            return true;
        } else if (si > 0.5) { // don't try to use Metaphone without some basic
            // JW match first
            return ctx.matchM3(w, tw);
        }
        return false;
    }

    /**
     * Make sentence match, ex 'Clint Eastwood', not just Clint
     *
     * @param ctx processing context
     * @param cs current sentence from context
     * @param tpls array of words ( from input sentence )
     * @param i current position in array upper
     * @return true if whole sentence matches, means both words from 'Clint
     * Eastwood' should exists in input sentence 'Michael likes movies with Jon
     * Way and Client East' in same order
     */
    private boolean matchSentense(PrContext ctx, ContextSentence cs, 
                                  String[] tpls, final int i) {

        if (i + cs.words.size() > tpls.length) {
            LOG.log( Level.INFO, "sentence not fully match because of EOL: i={0} ,words.size={1} ",
                    new Object[]{ i, cs.words.size()});
            return false;
        }

        int cf = 0;
        // from next word after first found and to end of words in context's sentence
        for (int j = i + 1; j < i + cs.words.size(); j++) {

            final String tw = tpls[j];

            for (Word w : cs.words) {
                if (matchW(ctx, w, tw)) {
                    ctx.addResult(tw, w.text); cf++;
                }
            }

        }
        return cf == cs.words.size() - 1; // if we found whole sentence
    }

    /**
     * Internal class, stores instances of text matching algorithms classes
     */
    private class PrContext {

        final JaroWinkler jw = new JaroWinkler(); 
        
        final Metaphone3 m3 = new Metaphone3();

        {
            m3.SetEncodeVowels(true); m3.SetEncodeExact(true);
        }
        
        final Map<String, String> results = new LinkedHashMap<>();

        boolean matchM3(Word w, String tw) {
            
            m3.SetWord(tw);m3.Encode();

            return (m3.GetMetaph() != null && w.metaph != null
                    && m3.GetMetaph().equals(w.metaph));
        }

        void addResult(String from, String to) {
            if (!from.equals(to)) {
                results.put(from, to); // avoid correction result
            }                          // if word exact matches one 
                                       //from context
        }
    }

    /**
     * Internal class, represents a sentence from context. Ex. Tom Cruise
     */
    private class ContextSentence {

        private final List<Word> words; // list of words in this sentence

        ContextSentence(String text, List<Word> words) {
            this.words = Collections.unmodifiableList(words);
        }
    }

    /**
     * Internal class, represents single word from sentence
     */
    private class Word {

        final String text, // word's text 
                     metaph; // Metaphone3 index

        Word(String text, String metaph) {
            this.text = text; this.metaph = metaph;
        }
    }

}
