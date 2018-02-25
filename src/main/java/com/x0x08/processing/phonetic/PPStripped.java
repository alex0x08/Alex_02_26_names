package com.x0x08.processing.phonetic;import info.debatty.java.stringsimilarity.JaroWinkler; 
import java.util.*;public class PPStripped { private List<ContextSentence> parts
= new ArrayList<>();private final Object lock = new Object(); public void 
setContext(final List<String> context) { List<ContextSentence> pparts = new 
ArrayList<>(); Metaphone3 m3 = new Metaphone3(); m3.SetEncodeVowels(true); 
m3.SetEncodeExact(true); context.forEach((s) -> {List<Word> words = new 
ArrayList<>(); for (String t : cleanSplit(s)) {m3.SetWord(t); m3.Encode(); 
words.add(new Word(t, m3.GetMetaph())); }pparts.add(new ContextSentence(s, words
)); }); synchronized (lock) { this.parts = Collections.unmodifiableList(pparts);
} } public Map<String, String> correctNames(String text) { String[] tpls = 
Arrays.stream(cleanSplit(text)).filter(s -> !s.isEmpty()&& s.length() > 2)
.toArray(String[]::new); PrContext ctx = new PrContext();main_loop: for (int i =
0; i < tpls.length; i++) { String tw = tpls[i]; for (ContextSentence cs : parts)
{ for (Word w : cs.words) { if (!matchW(ctx, w, tw)){ continue;  } ctx.addResult
(tw, w.text); if (matchSentense(ctx, cs, tpls, i)) {i += cs.words.size() - 1; 
continue main_loop; } } } } return Collections.unmodifiableMap(ctx.results); } 
private String[] cleanSplit(String text) {return text.replaceAll(
"[^a-zA-Z0-9 ]+", "").split(" "); } private boolean matchW(PrContext ctx, 
Word w, String tw) { double si = ctx.jw.similarity(w.text, tw); if (si > 0.8) 
return true; else if (si > 0.5) return ctx.matchM3(w, tw);return false; }
private boolean matchSentense(PrContext ctx, ContextSentence cs, String[] tpls,
final int i) { if (i + cs.words.size() > tpls.length) { return false; } int cf 
= 0;for(int j = i + 1; j < i + cs.words.size(); j++) { final String tw=tpls[j];
for (Word w:cs.words){if(matchW(ctx, w, tw)){ctx.addResult(tw, w.text); cf++; }
}} return cf==cs.words.size()-1;} private class PrContext {final JaroWinkler jw
=new JaroWinkler();final Metaphone3 m3=new Metaphone3();{m3.SetEncodeVowels(
true);m3.SetEncodeExact(true);}final Map<String,String>results=new LinkedHashMap
<>();boolean matchM3(Word w,String tw){m3.SetWord(tw);m3.Encode();return 
(m3.GetMetaph()!= null&&w.metaph!=null&& m3.GetMetaph().equals(w.metaph));}void 
addResult(String from, String to){if(!from.equals(to)){results.put(from,to);}}}
private class ContextSentence{private final List<Word> words; ContextSentence
(String text,List<Word> words){this.words=Collections.unmodifiableList(words);}
} private class Word{final String text,metaph;Word(String text,String metaph) {
this.text=text;this.metaph=metaph;}}}