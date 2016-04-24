package rpi.edu.transliterator.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import rpi.edu.transliterator.config.Default;
import rpi.edu.transliterator.model.Pair;
import rpi.edu.transliterator.model.Phrase;
import rpi.edu.transliterator.model.Token;
import rpi.edu.transliterator.module.*;
import rpi.edu.transliterator.module.States.State;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;

public class Server {
    private final static int PORT = 10086;

    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            OutputStream os = exchange.getResponseBody();

            JSONObject response = new JSONObject();
            response.put("error", false);

            String queryStr = exchange.getRequestURI().getQuery();
            State state = States.generateState();
            String key  = state.getKey();
            String time = state.getTime();
            String pairFile = time + ".pair.txt";

            States.setParameter(key, queryStr);

            boolean err = false;
            String line;
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("data/pair/" + pairFile), "utf-8"));
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                }
                br.close();
                bw.close();

            } catch (IOException ex) {
                err = true;
            }

            response.put("error", err);
            if (!err) {
                response.put("key", key);
                response.put("pair_file", pairFile);
                States.setPairFile(key, pairFile);
                States.setMsg(key, "pair file uploaded");
            }
            String responseStr = response.toString();

            exchange.sendResponseHeaders(200, responseStr.length());
            os.write(responseStr.getBytes());
            os.close();
        }
    }

    static class ProcHandle implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String queryStr = exchange.getRequestURI().getQuery();
            String key = null;
            for (String query : queryStr.split("&")) {
                String[] pair = query.split("=");
                if (pair[0].equals("key")) {
                    key = pair[1];
                    break;
                }
            }

            JSONObject response = new JSONObject();
            if (key == null) {
                response.put("error", true);
                response.put("msg", "key is not specified");
            } else {
                boolean procResult = Alignment.toNewsFormat(key, null);
                if (procResult) {
                    response.put("error", false);
                    States.setMsg(key, "pair file processed");
                } else {
                    response.put("error", true);
                    response.put("msg", "failed to process pair file");
                    States.setMsg(key, "failed to process pair file");
                }
            }
            String responseStr = response.toString();

            exchange.sendResponseHeaders(200, responseStr.length());
            OutputStream os = exchange.getResponseBody();
            os.write(responseStr.getBytes());
            os.close();
        }
    }

    static class AlignHandle implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String queryStr = exchange.getRequestURI().getQuery();
            String key = null;
            PropertiesConfiguration config = new PropertiesConfiguration();
            for (String query : queryStr.split("&")) {
                String[] pair = query.split("=");
                if (pair[0].equals("key")) {
                    key = pair[1];
                    break;
                } else {
                    config.setProperty(pair[0], pair[1]);
                }
            }

            JSONObject response = new JSONObject();
            Process pr = null;
            if (key == null) {
                response.put("error", true);
                response.put("msg", "key is not specified");
            } else {
                pr = Alignment.m2mAlign(key, config);
                if (pr == null) {
                    response.put("error", true);
                    response.put("msg", "failed to run m2m-aligner");
                    States.setMsg(key, "training failed");
                } else {
                    response.put("error", false);
                    response.put("msg", "pairs are being aligned");
                    States.setMsg(key, "training");
                }

            }
            String responseStr = response.toString();

            exchange.sendResponseHeaders(200, responseStr.length());
            OutputStream os = exchange.getResponseBody();
            os.write(responseStr.getBytes());
            os.close();

            try {
                int exitCode = pr.waitFor();
                if (exitCode == 0) {
                    // successful
                    States.setModelFile(key, States.getFilename(key));
                    States.setMsg(key, "training done");
                } else {
                    States.setMsg(key, "training failed");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Training API
     * Parameters:
     *   - name: Uploader
     *   - maxx: max character number (source)
     *   - maxy: max character number (targer)
     *   - cutoff: alignment cutoff
     *   - maxfn: joint, conXY or conYX
     *   - lang: language
     */
    static class TrainingHandle implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean err = false;
            JSONObject response = new JSONObject();
            String responseStr  = null;
            OutputStream os = exchange.getResponseBody();

            // query
            String query = exchange.getRequestURI().getQuery();
            PropertiesConfiguration config = new PropertiesConfiguration();
            for (String q : query.split("&")) {
                String[] pair = q.split("=");
                config.setProperty(pair[0], pair[1]);
            }

            // accept file
            State state = States.generateState();
            String key  = state.getKey();
            String time = state.getTime();
            String pairFile = time + ".pair.txt";
            String procFile = time + ".proc.txt";
            States.setParameter(key, query);

            String line;
            BufferedReader br;
            BufferedWriter bw;
            InputStream is = exchange.getRequestBody();
            try {
                br = new BufferedReader(new InputStreamReader(is, "utf-8"));
                bw = new BufferedWriter(new OutputStreamWriter
                        (new FileOutputStream("data/pair/" + pairFile), "utf-8"));
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    bw.newLine();
                }
                br.close();
                bw.close();
            } catch (IOException ex) {
                err = true;
            }

            response.put("error", err);
            if (!err) {
                response.put("key", key);
                response.put("pair_file", pairFile);
                States.setPairFile(key, pairFile);
                States.setMsg(key, "pair file uploaded");
            } else {
                responseStr = response.toString();
                exchange.sendResponseHeaders(200, responseStr.getBytes().length);
                os.write(responseStr.getBytes());
                os.close();
                return;
            }

            // process
            boolean procResult = Alignment.toNewsFormat(key, null);
            if (procResult) {
                States.setMsg(key, "pair file processed");
                response.put("proc_file", procFile);
            } else {
                response.put("error", true);
                response.put("msg", "failed to process pair file");
                States.setMsg(key, "failed to process pair file");
                responseStr = response.toString();
                exchange.sendResponseHeaders(200, responseStr.getBytes().length);
                os.write(responseStr.getBytes());
                os.close();
                return;
            }

            // training
            response.put("msg", "training");
            States.setMsg(key, "training");
            System.out.println("[" + key + "] training...");
            responseStr = response.toString();
            exchange.sendResponseHeaders(200, responseStr.getBytes().length);
            os.write(responseStr.getBytes());
            os.close();

            Trainer.launch(key, config);
//            Process process = Alignment.m2mAlign(key, config);
//            if (process != null) {
//                response.put("msg", "training");
//                States.setMsg(key, "training");
//                System.out.println("[" + key + "] traning...");
//            } else {
//                response.put("error", true);
//                response.put("msg", "traning failed");
//                States.setMsg(key, "training failed");
//            }
//
//            responseStr = response.toString();
//            exchange.sendResponseHeaders(200, responseStr.getBytes().length);
//            os.write(responseStr.getBytes());
//            os.close();
//
//            try {
//                int exitCode = process.waitFor();
//                if (exitCode == 0) {
//                    States.setModelFile(key, States.getFilename(key));
//                    States.setMsg(key, "training done");
//                    System.out.println("[" + key + "] traning done");
//                } else {
//                    States.setMsg(key, "training failed");
//                    System.out.println("[" + key + "] traning failed");
//                }
//            } catch (InterruptedException ex) {
//                States.setMsg(key, "traning failed");
//                ex.printStackTrace();
//            }
        }
    }

    /**
     * Transliteratioon API
     */
    static class TransliterateHandle implements HttpHandler {
        private Set<Character> separatorSet;

        public TransliterateHandle(PropertiesConfiguration config) {
            separatorSet = new HashSet<>();
            String tokenSeparator = (config == null)? Default.TOKEN_SEPARATOR_STRING
                    : config.getString("tokenSeparator", Default.TOKEN_SEPARATOR_STRING);
            for (char c : tokenSeparator.toCharArray()) {
                separatorSet.add(c);
            }
            separatorSet.add(' ');
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("---------------------------------------------");
            System.out.println("receive request: " + URLDecoder.decode(exchange.getRequestURI().toString(), "utf-8"));
            boolean err = false;
            JSONObject response = new JSONObject();
            String responseStr;

            // response
//            response.put("jscm", new JSONArray());
//            response.put("joint", new JSONArray());
//            response.put("entity", new JSONArray());

            // query
            String query = exchange.getRequestURI().getQuery();
            PropertiesConfiguration config = new PropertiesConfiguration();
            for (String q : query.split("&")) {
                String[] pair = q.split("=");
                config.setProperty(pair[0], pair[1]);
            }

            String key = config.getString("key");
            if (key == null) {
                response.put("error", true);
                response.put("msg", "model key not specified");
                System.out.println("no model key");
            } else {
                // segment
                String text = config.getString("query");
                System.out.println("query: " + text);
                if (text != null && text.length() > 0) {
                    Phrase phrase = new Phrase(text);

                    // split tokens
                    int startOffset = 0;
                    String cur = "";
                    for (int i = 0, l = text.length(); i < l; i++) {
                        if (cur.length() == 0) {
                            startOffset = i;
                        }

                        char c = text.charAt(i);
                        if (separatorSet.contains(c)) {
                            if (cur.length() > 0) {
                                phrase.addToken(new Token(cur, startOffset, i - 1));
                                cur = "";
                            }
                        } else {
                            cur += c;
                        }

                        if (i == l - 1 && cur.length() != 0) {
                            phrase.addToken(new Token(cur, startOffset, i));
                        }
                    }

                    // transliterate
                    List<Pair<String, Double>> phraseTransList
                            = new ArrayList<>();
                    List<Token> tokenList = phrase.getTokenList();
                    for (Token token : tokenList) {
                        List<Pair<String, Double>> transList
                                = Transliteration.transliterate(token.text(), key, config);

                        if (transList != null && transList.size() > 0) {
                            if (phraseTransList.size() == 0) {
                                phraseTransList = transList;
                            } else {
                                List<Pair<String, Double>> tmpPhraseTransList = new ArrayList<>();
                                for (Pair<String, Double> p1 : phraseTransList) {
                                    for (Pair<String, Double> p2 : transList) {
                                        tmpPhraseTransList.add(
                                                new Pair<>(p1.key + " " + p2.key,
                                                        p1.value * p2.value));
                                    }
                                }
                                phraseTransList = tmpPhraseTransList;
                            }
                        }
                    }
                    Collections.sort(phraseTransList, new Comparator<Pair<String, Double>>() {
                        @Override
                        public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                            return o2.value.compareTo(o1.value);
                        }
                    });
                    int finalSize = config.getInt("finalSize", Default.FINAL_NUMBER);
                    phraseTransList = phraseTransList.subList(0,
                            phraseTransList.size() < finalSize? phraseTransList.size() : finalSize);
                    System.out.print("jscm:\t");
                    for (Pair<String, Double> pair : phraseTransList) {
                        System.out.print(pair + " ");
                    }
                    System.out.println();

                    // link
                    double minValue = Double.MAX_VALUE;
                    double maxValue = -Double.MAX_VALUE;
                    double gap = -1;
                    for (Pair<String, Double> phraseTrans : phraseTransList) {
                        if (phraseTrans.value > maxValue) {
                            maxValue = phraseTrans.value;
                        }

                        if (phraseTrans.value < minValue) {
                            minValue = phraseTrans.value;
                        }
                    }
                    if (Double.compare(maxValue, minValue) != 0) {
                        gap = maxValue - minValue;
                    }

                    Map<String, Double> linkedEntities = new HashMap<>();
                    Map<String, Double> revisedMap = new HashMap<>();
                    for (Pair<String, Double> phraseTrans : phraseTransList) {
                        String trans = phraseTrans.key;
                        double score = phraseTrans.value;

                        // normalize
                        if (gap < 0) {
                            score = 1.0;
                        } else {
                            score = (score - minValue) / gap;
                        }
                        score = (1.0 - config.getDouble("bias", Default.BIAS)) * score
                                + config.getDouble("bias", Default.BIAS);

                        List<Pair<String, Double>> linkList = Linking.link(trans, config);
                        if (linkList != null) {
                            for (Pair<String, Double> linkPair : linkList) {
                                if (linkedEntities.containsKey(linkPair.key)) {
                                    linkedEntities.put(linkPair.key, linkPair.value);
                                } else {
                                    linkedEntities.put(linkPair.key, linkPair.value);
                                }
                            }
                        }
                        linkList = Linking.process(trans, linkList, config);
                        if (linkList != null && linkList.size() > 0) {
                            // linked
                            for (int i = 0, l = (linkList.size() <
                                    config.getInt("linkNumberThreshold", Default.LINK_NUMBER_THRESHOLD))?
                                    linkList.size()
                                    : config.getInt("linkNumberThreshold", Default.LINK_NUMBER_THRESHOLD);
                                 i < l; i++) {
                                String surface = linkList.get(i).key;
                                Pair<String, Double> revised = Revision.revise(trans, surface);
                                double sim_score = revised.value;
                                double joint_score = score * sim_score;

                                if (revisedMap.containsKey(revised.key)) {
                                    revisedMap.put(revised.key, revisedMap.get(revised.key) + joint_score);
                                } else {
                                    revisedMap.put(revised.key, joint_score);
                                }
                            }
                        } else {
                            if (revisedMap.containsKey(trans)) {
                                revisedMap.put(trans, revisedMap.get(trans)
                                        + score * config.getDouble("similarityThreshold",
                                        Default.SIMILARITY_THRESHOLD));
                            } else {
                                revisedMap.put(trans, score * config.getDouble("similarityThreshold",
                                        Default.SIMILARITY_THRESHOLD));
                            }
                        }
                    }


                    // entities
                    List<Pair<String, Double>> entityList = new ArrayList<>();
                    for (Map.Entry<String, Double> entry : linkedEntities.entrySet()) {
                        entityList.add(new Pair<>(entry.getKey(), entry.getValue()));
                    }
                    Collections.sort(entityList, new Comparator<Pair<String, Double>>() {
                        @Override
                        public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                            return o2.value.compareTo(o1.value);
                        }
                    });
                    entityList = entityList.subList(0, (entityList.size() < Default.ENTITY_NUM)?
                            entityList.size() : Default.ENTITY_NUM);
                    System.out.print("entity: ");
                    for (Pair<String, Double> pair : entityList) {
                        System.out.print(pair + " ");
                    }
                    System.out.println();

                    // revised
                    List<Pair<String, Double>> revisedTransList = new ArrayList<>();
                    for (Map.Entry<String, Double> entry : revisedMap.entrySet()) {
                        revisedTransList.add(new Pair<>(entry.getKey(), entry.getValue()));
                    }
                    Collections.sort(revisedTransList, new Comparator<Pair<String, Double>>() {
                        @Override
                        public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                            return o2.value.compareTo(o1.value);
                        }
                    });
                    if (finalSize != -1) {
                        revisedTransList = revisedTransList.subList(0, (revisedTransList.size() < finalSize) ?
                                revisedTransList.size() : finalSize);
                    }
                    System.out.print("joint: ");
                    for (Pair<String, Double> pair : revisedTransList) {
                        System.out.print(pair + " ");
                    }
                    System.out.println();

                    if (phraseTransList.size() == 1 && phraseTransList.get(0).key.trim().length() == 0) {
                        response.put("query", text);
                        response.put("error", true);
                        response.put("msg", "empty string");
                        JSONObject result = new JSONObject();
                        result.put("jscm", new JSONArray());
                        result.put("joint", new JSONArray());
                        result.put("entity", new JSONArray());
                        response.put("result", result);
                    } else {
                        JSONArray jscm = new JSONArray();
                        for (Pair<String, Double> pair : phraseTransList) {
                            JSONObject candidate = new JSONObject();
                            candidate.put(pair.key, pair.value);
                            jscm.put(candidate);
                        }
                        JSONArray joint = new JSONArray();
                        for (Pair<String, Double> pair : revisedTransList) {
                            JSONObject candidate = new JSONObject();
                            candidate.put(pair.key, pair.value);
                            joint.put(candidate);
                        }
                        JSONArray entity = new JSONArray();
                        for (Pair<String, Double> pair : entityList) {
                            JSONObject candidate = new JSONObject();
                            candidate.put(pair.key, pair.value);
                            entity.put(candidate);
                        }
                        JSONObject result = new JSONObject();
                        result.put("jscm", jscm);
                        result.put("joint", joint);
                        result.put("entity", entity);
                        response.put("result", result);
                        response.put("query", text);
                        response.put("error", false);
                    }
                } else {
                    System.out.println("empty query");
                    response.put("query", text);
                    response.put("error", true);
                    response.put("msg", "empty query");
                    JSONObject result = new JSONObject();
                    result.put("jscm", new JSONArray());
                    result.put("joint", new JSONArray());
                    result.put("entity", new JSONArray());
                    response.put("result", result);
                }
//                responseStr = response.toString();
//                exchange.sendResponseHeaders(200, responseStr.length());
//                os.write(responseStr.getBytes());
//                os.close();
            }
            OutputStream os = exchange.getResponseBody();
            responseStr = response.toString();
            exchange.sendResponseHeaders(200, responseStr.getBytes().length);
            os.write(responseStr.getBytes());
            os.close();
        }
    }

    static class QueryHandle implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            PropertiesConfiguration config = new PropertiesConfiguration();
            for (String q : query.split("&")) {
                String[] pair = q.split("=");
                config.setProperty(pair[0], pair[1]);
            }

            JSONObject response = new JSONObject();
            String lang = config.getString("lang");
            String key  = config.getString("key");
            boolean all = config.getBoolean("all", false);
            if (all) {
                List<State> stateList = States.getStateList();
                JSONArray states = new JSONArray();
                if (stateList != null) {
                    for (State state : stateList) {
                        states.put(state.toJSON());
                    }
                }
                response.put("error", false);
                response.put("states", states);
            } else if (key != null && key.trim().length() != 0) {
                State state = States.getState(key);
                response.put("error", false);
                if (state != null) {
                    response.put("state", state.toJSON());
                } else {
                    response.put("state", new JSONObject());
                }
            } else if (lang != null && lang.trim().length() != 0) {
                JSONArray states = new JSONArray();
                List<State> stateList = States.getLanguageState(lang);
                if (stateList != null) {
                    for (State state : stateList) {
                        states.put(state.toJSON());
                    }
                }
                response.put("error", false);
                response.put("states", states);
            } else {
                response.put("error", true);
                response.put("msg", "neither language nor key is specified");
            }
            String responseStr = response.toString();
            exchange.sendResponseHeaders(200, responseStr.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseStr.getBytes());
            os.close();
        }
    }

    static class ExtractHandle implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            PropertiesConfiguration config = new PropertiesConfiguration();
            for (String q : query.split("&")) {
                String[] pair = q.split("=");
                config.setProperty(pair[0], pair[1]);
            }

            JSONObject response = new JSONObject();
            String lang = config.getString("lang");
            if (lang != null && lang.trim().length() != 0) {
                State state = States.generateState();
                String key  = state.getKey();
                States.setParameter(key, query);
                config.setProperty("key", key);

                response.put("error", false);
                response.put("msg", "extracting");
                response.put("key", key);
            } else {
                response.put("error", true);
                response.put("msg", "language not specified");
            }
            String responseStr = response.toString();
            exchange.sendResponseHeaders(200, responseStr.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseStr.getBytes());
            os.close();

            if (lang != null && lang.trim().length() != 0) {
                WikiPairExtraction wpe = new WikiPairExtraction(config);
                wpe.extractPairs();
            }
        }
    }

    static class ProcessHandle implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean err = false;
            JSONObject response = new JSONObject();
            String responseStr  = null;
            OutputStream os = exchange.getResponseBody();

            // query
            String query = exchange.getRequestURI().getQuery();
            PropertiesConfiguration config = new PropertiesConfiguration();
            for (String q : query.split("&")) {
                String[] pair = q.split("=");
                config.setProperty(pair[0], pair[1]);
            }

            String key = config.getString("key");
            if (key == null || key.length() == 0) {
                response.put("error", true);
                response.put("msg", "key is not specified");
                responseStr = response.toString();
                exchange.sendResponseHeaders(200, responseStr.getBytes().length);
                os.write(responseStr.getBytes());
                os.close();
            } else {
                boolean clone = config.getBoolean("clone", false);
                State state;
                if (clone) {
                    state = States.cloneState(key);
                } else {
                    state = States.getState(key);
                }

                if (state == null) {
                    response.put("error", true);
                    response.put("msg", "key is not found");
                    responseStr = response.toString();
                    exchange.sendResponseHeaders(200, responseStr.getBytes().length);
                    os.write(responseStr.getBytes());
                    os.close();
                } else {
                    String time = state.getTime();
                    String procFile = time + ".proc.txt";
                    States.setParameter(key, query);

                    // process
                    boolean procResult = Alignment.toNewsFormat(key, null);
                    if (procResult) {
                        States.setMsg(key, "pair file processed");
                        response.put("proc_file", procFile);
                    } else {
                        response.put("error", true);
                        response.put("msg", "failed to process pair file");
                        States.setMsg(key, "failed to process pair file");
                        responseStr = response.toString();
                        exchange.sendResponseHeaders(200, responseStr.getBytes().length);
                        os.write(responseStr.getBytes());
                        os.close();
                        return;
                    }

                    // training
                    response.put("msg", "training");
                    States.setMsg(key, "training");
                    System.out.println("[" + key + "] training...");
                    responseStr = response.toString();
                    exchange.sendResponseHeaders(200, responseStr.getBytes().length);
                    os.write(responseStr.getBytes());
                    os.close();

                    Trainer.launch(key, config);

//                    Process process = Alignment.m2mAlign(key, config);
//                    if (process != null) {
//                        response.put("msg", "training");
//                        States.setMsg(key, "training");
//                        System.out.println("[" + key + "] traning...");
//                    } else {
//                        response.put("error", true);
//                        response.put("msg", "traning failed");
//                        States.setMsg(key, "training failed");
//                    }
//
//                    responseStr = response.toString();
//                    exchange.sendResponseHeaders(200, responseStr.getBytes().length);
//                    os.write(responseStr.getBytes());
//                    os.close();
//
//                    try {
//                        int exitCode = process.waitFor();
//                        if (exitCode == 0) {
//                            States.setModelFile(key, States.getFilename(key));
//                            States.setMsg(key, "training done");
//                            System.out.println("[" + key + "] traning done");
//                        } else {
//                            States.setMsg(key, "training failed");
//                            System.out.println("[" + key + "] traning failed");
//                        }
//                    } catch (InterruptedException ex) {
//                        States.setMsg(key, "traning failed");
//                        ex.printStackTrace();
//                    }
                }
            }
        }
    }

    static class Trainer {
        public static void launch(String key, PropertiesConfiguration config) {
            Subthread subthread = new Subthread(key, config);
            subthread.start();
        }

        private static class Subthread implements Runnable {
            private Thread t;
            String key;
            PropertiesConfiguration config;

            public Subthread(String key, PropertiesConfiguration config) {
                this.key = key;
                this.config = config;
            }

            @Override
            public void run() {
                Process process = Alignment.m2mAlign(key, config);
                try {
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        States.setModelFile(key, States.getFilename(key));
                        States.setMsg(key, "training done");
                        System.out.println("[" + key + "] traning done");
                    } else {
                        States.setMsg(key, "training failed");
                        System.out.println("[" + key + "] traning failed");
                    }
                } catch (InterruptedException ex) {
                    States.setMsg(key, "training failed");
                    ex.printStackTrace();
                }
            }

            public void start ()
            {
                if (t == null)
                {
                    t = new Thread (this);
                    t.start();
                }
            }
        }
    }

    public void start() throws IOException {
        // load states
        States.load();

        FileHandler pairFileUploadHandler = new FileHandler();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/train", new TrainingHandle());
        server.createContext("/trans", new TransliterateHandle(null));
        server.createContext("/query", new QueryHandle());
        server.createContext("/extract", new ExtractHandle());
        server.createContext("/process", new ProcessHandle());
        server.setExecutor(null);
        server.start();
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }
}
