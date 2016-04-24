package rpi.edu.transliterator.module;

import info.debatty.java.stringsimilarity.Levenshtein;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONObject;
import rpi.edu.transliterator.model.Pair;
import rpi.edu.transliterator.module.States.State;

import java.io.*;
import java.text.Normalizer;
import java.util.*;

public class WikiPairExtraction {
    private PropertiesConfiguration config;

    public WikiPairExtraction(PropertiesConfiguration config) {
        this.config = config;
    }


    /**
     * Romanization (for non-Latin scripts)
     * @param str
     * @return
     */
    public String uroman(String str) throws IOException {
        Process proc = Runtime.getRuntime().exec("lib/uroman-v0.3/bin/uroman.pl -q " + str);
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));
        str = stdInput.readLine();
        stdInput.close();
        return str;
    }


    public List<String> match(String str) throws IOException {
        if (str == null) {
            return null;
        }

        double similarityThreshold = config.getDouble("similarity_threshold", 0.65);
        double metaphoneDistanceThreshold = config.getDouble("metaphone_threshold", 0.9);
        String punc = config.getString("punctuation", "-,()\\[\\]{}/<>:");
        String[] segments = str.split("\t");
        String source = "";
        String target = "";
        if (segments.length == 2) {
            source = segments[1];
            target = segments[0];
        } else if (segments.length == 3) {
            source = segments[2];
            target = segments[0];
        }

        if (config.getBoolean("enable_uroman", false)) {
            target = uroman(target);
        }

        NormalizedLevenshtein normLev = new NormalizedLevenshtein();
        Levenshtein lev = new Levenshtein();
        Metaphone metaphone = new Metaphone();

        String[] sourceTokens = source.split("[" + punc + "]");
        String[] targetTokens = target.split("[" + punc + "]");
        List<Token> sourceTokenList = new ArrayList<>();
        List<Token> targetTokenList = new ArrayList<>();
        for (String sourceToken : sourceTokens) {
            boolean digit = false;
            for (char c : sourceToken.toCharArray()) {
                if (c >= '0' && c <= '9') {
                    digit = true;
                    break;
                }
            }
            if (digit) {
                continue;
            }

            Token token = new Token();
            token.text = sourceToken;
            token.normText = Normalizer.normalize(sourceToken, Normalizer.Form.NFD)
                    .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase();
            token.metaphone = metaphone.encode(token.normText);
            sourceTokenList.add(token);
        }
        for (String targetToken : targetTokens) {
            boolean digit = false;
            for (char c : targetToken.toCharArray()) {
                if (c >= '0' && c <= '9') {
                    digit = true;
                    break;
                }
            }
            if (digit) {
                continue;
            }

            Token token = new Token();
            token.text = targetToken;
            token.normText = Normalizer.normalize(targetToken, Normalizer.Form.NFD)
                    .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase();
            token.metaphone = metaphone.encode(token.normText);
            targetTokenList.add(token);
        }

        if (sourceTokenList.size() == 0 || targetTokenList.size() == 0) {
            return null;
        }

        for (int i = 0; i < 3; i++) {
            for (Token sourceToken : sourceTokenList) {
                for (Token targetToken : targetTokenList) {
                    double stringSimilarity = normLev.similarity(sourceToken.normText, targetToken.normText);
                    double metaphoneDistance = lev.distance(sourceToken.metaphone, targetToken.metaphone);
                    if (stringSimilarity > similarityThreshold && metaphoneDistance < metaphoneDistanceThreshold) {
                        if (stringSimilarity > sourceToken.similarity) {
                            if ((targetToken.matched && stringSimilarity > targetToken.similarity) || !targetToken.matched) {
                                sourceToken.unbind();
                                sourceToken.bind(targetToken, stringSimilarity);
                            }
                        }
                    }
                }
            }
        }

        List<String> pairList = new ArrayList<>();
        for (Token token : sourceTokenList) {
            if (token.matched) {
                pairList.add(token.text + "\t" + token.matchedToken.text);
            }
        }
        return pairList;
    }


    public void extractPairs() throws IOException {
        String languageCode = config.getString("lang");
        System.out.println("language: " + languageCode);
        String line;

        // load stop words
        Set<String> stopwordSet = new HashSet<>();
        BufferedReader swReader = new BufferedReader(new InputStreamReader(new FileInputStream("config/stopword.txt"), "utf-8"));
        while ((line = swReader.readLine()) != null) {
            stopwordSet.add(line);
        }
        swReader.close();

        Map<String, Integer> pairCountMap = new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/wiki_name_pairs.txt"), "utf-8"));
        while ((line = br.readLine()) != null) {
            if (!line.split("\t")[1].equals(languageCode)) {
                continue;
            }

            // normalize string
            line = line.replace("\\'", "'").replace("’", "'");

            List<String> pairList = match(line);
            if (pairList != null) {
                for (String pair : pairList) {
                    if (pairCountMap.containsKey(pair)) {
                        pairCountMap.put(pair, pairCountMap.get(pair) + 1);
                    } else {
                        pairCountMap.put(pair, 1);
                    }
                }
            }
        }

        List<Pair<String, Integer>> identicalPairList = new ArrayList<>();
        List<Pair<String, Integer>> similarPairList   = new ArrayList<>();
        List<Pair<String, Integer>> stopwordPairList  = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pairCountMap.entrySet()) {
            String[] pair = entry.getKey().split("\t");
            if (stopwordSet.contains(pair[0].toLowerCase()) || stopwordSet.contains(pair[1].toLowerCase())) {
                stopwordPairList.add(new Pair<>(entry.getKey(), entry.getValue()));
            } else if (pair[0].replace(".", "").equalsIgnoreCase(pair[1].replace(".", ""))) {
                identicalPairList.add(new Pair<>(entry.getKey(), entry.getValue()));
            } else {
                similarPairList.add(new Pair<>(entry.getKey(), entry.getValue()));
            }
        }
        System.out.println("#stopword:  " + stopwordPairList.size());
        System.out.println("#identical: " + identicalPairList.size());
        System.out.println("#similar:   " + similarPairList.size());

        double ratio = config.getDouble("dataIdenticalSimilarRatio", 0.1);
        int identicalNumber = (int) (ratio * similarPairList.size());
        Collections.shuffle(identicalPairList);

        String key = config.getString("key");
        String time = States.getTime(key);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
//                new FileOutputStream("data/pair/" + languageCode + "_wiki.txt"), "utf-8"));
                new FileOutputStream("data/pair/" + time + ".pair.txt"), "utf-8"));
        for (Pair<String, Integer> pair : similarPairList) {
            bw.write(pair.key);
            bw.newLine();
        }
        for (int i = 0, l = (identicalNumber < identicalPairList.size())? identicalNumber : identicalPairList.size();
             i < l; i++) {
            bw.write(identicalPairList.get(i).key);
            bw.newLine();
        }
        bw.close();

        States.setPairFile(key, time + ".pair.txt");
        States.setMsg(key, "pair file extracted");
        String parameter = States.getParameter(key);
        if (parameter == null || parameter.length() == 0) {
            parameter = "identical=" + identicalNumber + "&similar=" + similarPairList.size();
        } else {
            parameter += "&identical=" + identicalNumber + "&similar=" + similarPairList.size();
        }
        States.setParameter(key, parameter);

//        JSONObject json = new JSONObject();
//        json.put("identical", identicalNumber);
//        json.put("similar", similarPairList.size());
//        return json;
    }

    private static class Token {
        String text;
        String normText;
        String metaphone;
        boolean matched = false;
        double similarity = 0.0;
        Token matchedToken = null;

        public void bind(Token targetToken, double similarity) {
            this.similarity = similarity;
            matchedToken = targetToken;
            matched = true;

            targetToken.similarity = similarity;
            targetToken.matchedToken = this;
            targetToken.matched = true;
        }

        public void unbind() {
            if (matched) {
                matchedToken.matched = false;
                matchedToken.similarity = 0.0;
                matchedToken.matchedToken = null;

                matched = false;
                similarity = 0.0;
                matchedToken = null;
            }
        }
    }

    public static void main(String[] args) {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.setProperty("enable_uroman", true);
        WikiPairExtraction wpe = new WikiPairExtraction(config);
        try {
            wpe.extractPairs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
