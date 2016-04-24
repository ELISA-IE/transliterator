package rpi.edu.transliterator.module;

import org.apache.commons.configuration.PropertiesConfiguration;
import rpi.edu.transliterator.config.Default;
import rpi.edu.transliterator.model.Pair;
import rpi.edu.transliterator.util.ArrayUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

public class Transliteration {
    private static Map<String, Model> keyModelMap = new HashMap<>();

    private static class Model {
        long lastAccessTime;
        int maxGramNumber = 3;
        int totalUnitCount = 0;
        Map<String, Integer> ngramCountMap;
        Map<String, Set<String>> transliterationMap;
        Map<String, Integer> uniqueNextMap;

        public Model() {
            lastAccessTime = System.currentTimeMillis();
            ngramCountMap = new HashMap<>();
            uniqueNextMap = new HashMap<>();
            transliterationMap = new HashMap<>();
        }

        public void updateAccessTime() {
            lastAccessTime = System.currentTimeMillis();
        }
    }

    private static class State {
        public double score = 1.0;
        public String[][] units;
        public int currentIndex = -1;

        public State() {
            String[][] units = {{Default.START_SYMBOL, Default.START_SYMBOL}};
            this.units = units;
        }

        /**
         * Calculate the Jelinked-Mercer smoothed score of the last unit
         * @return
         */
        public double jelinekMercerSmoothedScore(Model model) {
            int unitNum = units.length;
            int currentIndex = unitNum - 1;
            int gramNumber = (unitNum < model.maxGramNumber)? unitNum : model.maxGramNumber;
            double[] probs = new double[gramNumber];

            String currentUnit = units[currentIndex][0] + Default.PAIR_DELIMITER
                    + units[currentIndex][1];
            for (int i = 0; i < gramNumber; i++) {
                if (i == 0) {
                    if (model.ngramCountMap.containsKey(currentUnit)) {
                        int unitCount = model.ngramCountMap.get(currentUnit);
                        probs[0] = (double) unitCount / model.totalUnitCount;
                    } else {
                        probs[0] = 0;
                    }
                } else {
                    String history = "";
                    for (int j = currentIndex - i; j < currentIndex; j++) {
                        if (history.length() == 0) {
                            history = units[j][0] + Default.PAIR_DELIMITER
                                    + units[j][1];
                        } else {
                            history = Default.SEGMENT_DELIMITER + units[j][0]
                                    + Default.PAIR_DELIMITER + units[j][1];
                        }
                    }

                    String ngram = history + Default.SEGMENT_DELIMITER
                            + units[currentIndex][0] + Default.PAIR_DELIMITER
                            + units[currentIndex][1];

                    int historyCount = model.ngramCountMap.containsKey(history)?
                            model.ngramCountMap.get(history) : 0;
                    int ngramCount = model.ngramCountMap.containsKey(ngram)?
                            model.ngramCountMap.get(ngram) : 0;
                    double pML = (historyCount != 0 && ngramCount != 0)?
                            (double) ngramCount / historyCount : 0.0;

                    /* Calculate lambda */
                    int historyNextCount = model.uniqueNextMap.containsKey(history)?
                            model.uniqueNextMap.get(history) : 0;
                    double ratio = (historyNextCount != 0 && historyCount != 0)?
                            (double) historyCount / historyNextCount : 0.0;
                    double lambda = 0.5 + ratio * 0.1;
                    lambda = (lambda > 1.0)? 1.0 : lambda;

                    probs[i] = lambda * pML + (1 - lambda) * probs[i - 1];
                }
            }

            return probs[gramNumber - 1];
        }
    }

    private static boolean loadModel(String key, PropertiesConfiguration config) {
        if (States.hasKey(key)) {
            String modelFile = States.getModelFile(key);

            if (modelFile == null || modelFile.trim().length() == 0) {
                return false;
            }

            boolean ignoreCase = config != null
                    && config.getBoolean("ignoreCase", false);
            int maxGramNumber  = (config == null)? 3
                    : config.getInt("maxGramNumber", 3);

            String line;
            BufferedReader br;
            int totalUnitCount = 0;
            try {
                Model model = new Model();
                br = new BufferedReader(new InputStreamReader(
                        new FileInputStream("data/model/" + modelFile), "utf-8"));
                while ((line = br.readLine()) != null) {
                    // split the aligned pair
                    String[] pair = line.replace(":", "").split("\t");
                    String[] sourceUnits = pair[0].split("\\|");
                    String[] targetUnits;
                    if (ignoreCase) {
                        targetUnits = pair[1].toLowerCase().split("\\|");
                    } else {
                        targetUnits = pair[1].split("\\|");
                    }

                    // check unit numbers
                    if (sourceUnits.length != targetUnits.length) {
                        continue;
                    }

                    // add start and end symbols
                    sourceUnits = ArrayUtils.prepend(sourceUnits, Default.START_SYMBOL);
                    targetUnits = ArrayUtils.prepend(targetUnits, Default.START_SYMBOL);
                    sourceUnits = ArrayUtils.append(sourceUnits, Default.END_SYMBOL);
                    targetUnits = ArrayUtils.append(targetUnits, Default.END_SYMBOL);

                    int unitNumber = sourceUnits.length;
                    totalUnitCount += unitNumber;

                    // add transliterations
                    for (int i = 0; i < unitNumber; i++) {
                        if (model.transliterationMap.containsKey(sourceUnits[i])) {
                            model.transliterationMap.get(sourceUnits[i]).add(targetUnits[i]);
                        } else {
                            Set<String> targetSet = new HashSet<>();
                            targetSet.add(targetUnits[i]);
                            model.transliterationMap.put(sourceUnits[i], targetSet);
                        }
                    }

                    /* count ngrams */
                    for (int gramNum = 1; gramNum <= maxGramNumber; gramNum++) {
                        for (int beginIndex = 0; beginIndex <= unitNumber - gramNum; beginIndex++) {
                            String ngram = "";

                            for (int unitIndex = beginIndex, endIndex = gramNum + beginIndex;
                                 unitIndex < endIndex; unitIndex++) {
                                if (ngram.length() == 0) {
                                    ngram = sourceUnits[unitIndex] + Default.PAIR_DELIMITER + targetUnits[unitIndex];
                                } else {
                                    ngram += Default.SEGMENT_DELIMITER + sourceUnits[unitIndex] +
                                            Default.PAIR_DELIMITER + targetUnits[unitIndex];
                                }
                            }

                            if (model.ngramCountMap.containsKey(ngram)) {
                                model.ngramCountMap.put(ngram, model.ngramCountMap.get(ngram) + 1);
                            } else {
                                model.ngramCountMap.put(ngram, 1);
                            }
                        }
                    }
                }
                br.close();

                // unique next
                Map<String, Set<String>> uniqueNextSetMap = new HashMap<>();
                for (Map.Entry<String, Integer> entry : model.ngramCountMap.entrySet()) {
                    String ngram = entry.getKey();
                    int unitNum = ngram.split(Default.SEGMENT_DELIMITER).length;
                    if (unitNum > 1) {
                        String history = ngram.substring(0, ngram.lastIndexOf(Default.SEGMENT_DELIMITER_CHAR));
                        String next = ngram.substring(ngram.lastIndexOf(Default.SEGMENT_DELIMITER_CHAR) + 1);

                        if (uniqueNextSetMap.containsKey(history)) {
                            uniqueNextSetMap.get(history).add(next);
                        } else {
                            Set<String> set = new HashSet<>();
                            set.add(next);
                            uniqueNextSetMap.put(history, set);
                        }
                    }
                }
                for (Map.Entry<String, Set<String>> entry : uniqueNextSetMap.entrySet()) {
                    model.uniqueNextMap.put(entry.getKey(), entry.getValue().size());
                }

                model.totalUnitCount = totalUnitCount;
                model.maxGramNumber  = maxGramNumber;
                model.updateAccessTime();
                keyModelMap.put(key, model);
                return true;
            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public static List<Pair<String, Double>> transliterate(String text,
                                                           String key,
                                                           PropertiesConfiguration config) {
        if (text == null || text.trim().length() == 0) {
            return null;
        }

        if (!(keyModelMap.containsKey(key) || States.hasKey(key))) {
            return null;
        }

        // check model
        Model model;
        if (keyModelMap.containsKey(key)) {
            model = keyModelMap.get(key);
        } else {
            boolean modelLoad = loadModel(key, config);
            if (modelLoad) {
                model = keyModelMap.get(key);
            } else {
                return null;
            }
        }

        // get parameters
        int trimNumber  = config.getInt("trimNumber", Default.TRIM_NUMBER);
        int beamSize    = config.getInt("beamSize", Default.BEAM_SIZE);
        int finalNumber = config.getInt("finalNumber", Default.FINAL_NUMBER);

        // pre-process
        text = text.trim() + Default.END_SYMBOL;

        char[] chars = text.toCharArray();
        int endIndex = chars.length - 1;
        List<State> stateList = new ArrayList<>();
        stateList.add(new State());
        while(true) {
            boolean finish = true;
            for (State state : stateList) {
                if (state.currentIndex != endIndex) {
                    finish = false;
                    break;
                }
            }
            if (finish) {
                break;
            }

            List<State> tmpStateList = new ArrayList<>();
            for (State state : stateList) {
                int currentIndex = state.currentIndex;
                if (currentIndex == endIndex) {
                    tmpStateList.add(state);
                    continue;
                }

                for (int gramNum = 1; gramNum <= model.maxGramNumber; gramNum++) {
                    int newIndex = currentIndex + gramNum;
                    if (newIndex >= endIndex && gramNum != 1) {
                        break;
                    }

                    String source = "";
                    for (int i = 0; i < gramNum; i++) {
                        source += chars[currentIndex + 1 + i];
                    }
                    List<String> targetList = new ArrayList<>();
                    if (model.transliterationMap.containsKey(source)) {
                        targetList.addAll(model.transliterationMap.get(source));
                    } else {
                        targetList.add(Default.VOID_TRANSLITERTION);
                    }

                    for (String target : targetList) {
                        State newState = new State();
                        String[] unit = new String[2];
                        unit[0] = source;
                        unit[1] = target;
                        newState.units = ArrayUtils.append(state.units, unit);
                        newState.currentIndex = currentIndex + gramNum;
                        newState.score = state.score * newState.jelinekMercerSmoothedScore(model);
                        tmpStateList.add(newState);
                    }

                }
            }
            stateList = tmpStateList;

            if (stateList.size() > trimNumber) {
                Collections.sort(stateList, new Comparator<State>() {
                    @Override
                    public int compare(State o1, State o2) {
                        return Double.compare(o2.score, o1.score);
                    }
                });
                stateList = stateList.subList(0, beamSize);
            }
        }

        stateList = stateList.subList(0, (stateList.size() < finalNumber)?
                stateList.size() : finalNumber);
        List<Pair<String, Double>> transliterationList = new ArrayList<>();
        for (State state : stateList) {
            String transliteration = "";
            for (int i = 1; i < state.units.length - 1; i++) {
                transliteration += state.units[i][1];
            }
            transliterationList.add(
                    new Pair<>(transliteration.replace(Default.VOID_TRANSLITERTION, ""),
                            state.score));
        }

        model.updateAccessTime();
        return transliterationList;
    }
}
