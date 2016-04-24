package rpi.edu.transliterator.module;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rpi.edu.transliterator.config.Default;
import rpi.edu.transliterator.model.Pair;
import rpi.edu.transliterator.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Linking {
    private static Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static NormalizedLevenshtein normLev = new NormalizedLevenshtein();

    private static Set<String> typeSet;

    /**
     * Load type list
     * @param config configuration
     * @return
     */
    public static boolean loadTypes(PropertiesConfiguration config) {
        String line;
        typeSet = new HashSet<>();
        String typeListFile = (config == null)? Default.TYPE_LIST_FILE :
                config.getString("typeListFile", Default.TYPE_LIST_FILE);

        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(typeListFile), "utf-8"));
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {
                    typeSet.add(line);
                }
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * Read the response in JSON format of a URL
     * @param url request URL
     * @return
     * @throws IOException
     */
    public static final JSONObject readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();

            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            String jsonText = sb.toString();

            JSONObject json = new JSONObject(jsonText);
            return json;
        } catch (JSONException jex) {
            return null;
        } finally {
            is.close();
        }
    }

    /**
     * Retrieve linking results
     * @param query linking query
     * @param related related mentions
     * @return a list of linked entities
     */
    public static List<Pair<String, Double>> link(String query, List<String> related,
                                           PropertiesConfiguration config) {
        try {
            String linkingUrl = (config == null)? Default.LINKING_URL :
                    config.getString("linkingUrl", Default.LINKING_URL);
            String url = linkingUrl
                    + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            if (related != null && related.size() > 0) {
                for (String r : related) {
                    url += "," + URLEncoder.encode(r, StandardCharsets.UTF_8.toString());
                }
            }
            JSONObject response = readJsonFromUrl(url);
            if (response == null) {
                return null;
            }
            if (response.has("results")
                    && response.getJSONArray("results").getJSONObject(0).has("annotations")) {
                List<Pair<String, Double>> linkList = new ArrayList<>();
                JSONArray annotations = response.getJSONArray("results")
                        .getJSONObject(0).getJSONArray("annotations");
                for (Object anno : annotations) {
                    JSONObject annoJson = (JSONObject)anno;
                    String annoUrl = URLDecoder.decode(annoJson.getString("url"),
                            StandardCharsets.UTF_8.toString());
                    String annoScore = annoJson.getString("score");
                    if (!annoScore.equals("NaN") && !annoUrl.equals("NIL")) {
                        linkList.add(new Pair<>(annoUrl, Double.parseDouble(annoScore)));
                    }
                }
                return linkList;
            } else {
                return null;
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieve linking results (without context)
     * @param query linking query
     * @return a list of linked entities
     */
    public static List<Pair<String, Double>> link(String query, PropertiesConfiguration config) {
        return link(query, null, config);
    }

    public static List<Pair<String, Double>> process(String query, List<Pair<String, Double>> list,
                                              PropertiesConfiguration config) {
        List<Pair<String, Double>> processedList = new ArrayList<>();

        // get parameters
        int linkNumberThreshold = (config == null)? Default.LINK_NUMBER_THRESHOLD :
                config.getInt("linkNumberThreshold", Default.LINK_NUMBER_THRESHOLD);
        double linkScoreThreshold = (config == null)? Default.LINK_SCORE_THRESHOLD :
                config.getDouble("linkScoreThreshold", Default.LINK_SCORE_THRESHOLD);
        double similarityThreshold = (config == null)? Default.SIMILARITY_THRESHOLD :
                config.getDouble("similarityThreshold", Default.SIMILARITY_THRESHOLD);
        boolean typeFilter = (config == null)? Default.TYPE_FILTER:
                config.getBoolean("typeFilter", Default.TYPE_FILTER);

        for (Pair<String, Double> pair : list) {
            if (processedList.size() >= linkNumberThreshold) {
                break;
            }

            // filtering linked entities with linking score
            double score = pair.value;
            if (score < linkScoreThreshold) {
                continue;
            }

            // filtering linked entities with type
            if (typeFilter
                    && !checkType(type(pair.key.substring(
                    pair.key.lastIndexOf('/') + 1, pair.key.length() - 1), config),
                    Default.TYPE_PREFIX, true)) {
                continue;
            }

            // filtering linked entities with similarity with transliteration hypothesis
            String title = extractTitle(pair.key);
            double highestSimilarity = 0.0;
            String[] titleTokens;
            if (title != null) {
                titleTokens = StringUtils.normalizeString(title.split(" "));
            } else {
                continue;
            }
            String[] queryTokens = StringUtils.normalizeString(query.split(" "));
            for (String titleToken : titleTokens) {
                for (String queryToken : queryTokens) {
                    double similarity = normLev.similarity(titleToken, queryToken);
                    if (titleToken.length() < 3 && queryToken.length() < 3) {
                        similarity = 0.0;
                    }
                    if (similarity > highestSimilarity) {
                        highestSimilarity = similarity;
                    }
                }
            }
            if (highestSimilarity >= similarityThreshold) {
                processedList.add(new Pair<>(title, score));
            }
        }

        return processedList;
    }

    /**
     * Retrieve entity types
     * @param query entity title
     * @return a list of types
     */
    public static List<String> type(String query, PropertiesConfiguration config) {
        try {
            String typingUrl = (config == null)? Default.TYPING_URL :
                    config.getString("typingUrl", Default.TYPING_URL);

            String url = typingUrl + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            JSONObject response = Request.readJsonFromUrl(url);
            if (response == null) {
                return null;
            }

            if (response.has("results")) {
                JSONArray typeArray = response.getJSONArray("results");
                if (typeArray.length() > 0) {
                    List<String> typeList = new ArrayList<>();
                    for (Object typeObject : typeArray) {
                        String type = ((JSONObject) typeObject).getString("type");
                        typeList.add(type);
                    }
                    return typeList;
                } else {
                    return null;
                }
            } else {
                return null;
            }

        } catch (IOException ioex) {
            ioex.printStackTrace();
            return null;
        }
    }

    /**
     * Extract title from url (with angle brackets)
     * @param url kb url
     * @return title string
     */
    public static String extractTitle(String url) {
        int slashIndex = url.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex != url.length() - 1) {
            String title = url.substring(slashIndex + 1, url.length() - 1);
            title = title.replace("_", " ").replace(",", " ");
            title = title.replaceAll("\\([^\\(\\)]*\\)", "").trim();
            title = Normalizer.normalize(title, Normalizer.Form.NFD);
            title = pattern.matcher(title).replaceAll("");
            return title;
        } else {
            return null;
        }
    }


    /**
     * Check if the type list contains any type in the predefined set
     * @param typeList type list
     * @param prefix prefix string, e.g., "http://dbpedia.org/ontology/"
     * @param removePrefix to remove the prefix before matching or not
     * @return
     */
    public static boolean checkType(List<String> typeList, String prefix,
                                    boolean removePrefix) {
        if (typeList == null || typeList.size() == 0) {
            return false;
        }

        for (String type : typeList) {
            if (type.startsWith(prefix)) {
                if (removePrefix && typeSet.contains(type.substring(prefix.length()))) {
                    return true;
                } else if (!removePrefix && typeSet.contains(type)) {
                    return true;
                }
            }
        }

        return false;
    }
}
