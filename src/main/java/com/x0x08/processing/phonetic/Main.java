package com.x0x08.processing.phonetic;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * 
 * 
 * @author alex
 */
public class Main {

    public static void main(String[] args) {

        PostProcessor pp = new PostProcessor();

        pp.setContext(Arrays.asList("John Wayne",
                "Tom Hanks",
                "Tom Cruise",
                "Clint Eastwood", "Jon Hamm", "John Nolan", "William",
                "Fitcher"));

        String sample1 = "tomorrow I have a meeting with Willam and Tim Hanks Tom Crus and Eastwud";
        String sample2 = "Jonn invited me Jon Ham and Jon Wane, over for a lunch";

        String sample3 = "Michael likes movies with Jon Way and Client East”";

        doTest(pp,sample1);
        doTest(pp,sample2);
        doTest(pp,sample3);
        
        
    }

    private static void doTest(PostProcessor pp, String sample) {

        System.out.println("sample: "+sample);
        
        Map<String, String> out = pp.correctNames(sample);

        for (Entry<String, String> e : out.entrySet()) {
            System.out.println("suggested fix: " + e.getKey() + " | " + e.getValue());
        }

    }

}
