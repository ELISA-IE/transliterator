package rpi.edu.transliterator.module;

import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class States {
    private static List<State> stateList = new ArrayList<>();

    public static boolean load() {
        String line;
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream("config/state_list.txt"), "utf-8"));
            while ((line = br.readLine()) != null) {
                State state = new State(line);
                stateList.add(state);
                System.out.println(line);
            }
            br.close();
        } catch (IOException ex) {
            System.err.println("Failed to load state list. Use empty list.");
            stateList = new ArrayList<>();
            return false;
        }
        return true;
    }

    public static boolean update() {
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("config/state_list.txt"), "utf-8"));
            for (State state : stateList) {
                bw.write(state.toJSON().toString());
                bw.newLine();
            }
            bw.close();
        } catch (IOException ex) {
            System.err.println("Failed to write state list.");
            return false;
        }
        return true;
    }

    public static String getPairFile(String key) {
        String pairFile = null;
        for (State state : stateList) {
            if (state.key.equals(key)) {
                pairFile = state.pairFile;
                break;
            }
        }
        return pairFile;
    }

    public static boolean setPairFile(String key, String pairFile) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                state.pairFile = pairFile;
                update();
                return true;
            }
        }

        return false;
    }

    public static String getProcFile(String key) {
        String procFile = null;
        for (State state : stateList) {
            if (state.key.equals(key)) {
                procFile = state.procFile;
                break;
            }
        }
        return procFile;
    }

    public static boolean setProcFile(String key, String procFile) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                state.procFile = procFile;
                update();
                return true;
            }
        }
        return false;
    }

    public static String getModelFile(String key) {
        String modelFile = null;
        for (State state : stateList) {
            if (state.key.equals(key)) {
                modelFile = state.modelFile;
                break;
            }
        }
        return modelFile;
    }

    public static boolean setModelFile(String key, String modelFile) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                state.modelFile = modelFile;
                update();
                return true;
            }
        }
        return false;
    }

    public static String getTime(String key) {
        String time = null;
        for (State state : stateList) {
            if (state.key.equals(key)) {
                time = state.time;
                break;
            }
        }
        return time;
    }

    public static boolean setTime(String key, String time) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                state.time = time;
                update();
                return true;
            }
        }
        return false;
    }

    public static String getParameter(String key) {
        String parameter = null;
        for (State state : stateList) {
            if (state.key.equals(key)) {
                parameter = state.parameter;
                break;
            }
        }
        return parameter;
    }

    public static boolean setParameter(String key, String parameter) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                state.parameter = parameter;
                update();
                return true;
            }
        }
        return false;
    }

    public static String getMsg(String key) {
        String msg = null;
        for (State state : stateList) {
            if (state.key.equals(key)) {
                msg = state.msg;
                break;
            }
        }
        return msg;
    }

    public static boolean setMsg(String key, String msg) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                state.msg = msg;
                update();
                return true;
            }
        }
        return false;
    }

    public static boolean hasKey(String key) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static String getFilename(String key) {
        String filename = null;
        for (State state : stateList) {
            if (state.key.equals(key)) {
                String name   = "Unknown";
                String maxX   = "2";
                String maxY   = "2";
                String cutoff = "0.001";
                String manFn  = "joint";
                String lang   = "unknown";
                String parameter = state.parameter;
                if (parameter == null || parameter.length() == 0) {
                    break;
                }
                for (String p : parameter.split("&")) {
                    String[] pair = p.split("=");
                    if (pair[0].equalsIgnoreCase("name")) {
                        name = pair[1];
                    } else if (pair[0].equalsIgnoreCase("maxx")) {
                        maxX = pair[1];
                    } else if (pair[0].equalsIgnoreCase("maxy")) {
                        maxY = pair[1];
                    } else if (pair[0].equalsIgnoreCase("cutoff")) {
                        cutoff = pair[1];
                    } else if (pair[0].equalsIgnoreCase("maxFn")) {
                        manFn = pair[1];
                    } else if (pair[0].equalsIgnoreCase("lang")) {
                        lang = pair[1];
                    }
                }
                filename = name + "_" + state.time + ".x" + maxX + "y" + maxY
                        + "." + cutoff.replace(".", "_") + "." + manFn
                        + "." + lang + ".model.txt";
                break;
            }
        }
        return filename;
    }

    public static List<State> getLanguageState(String lang) {
        List<State> states = new ArrayList<>();
        for (State state : stateList) {
            String parameter = state.parameter;
            if (parameter == null || parameter.length() == 0) {
                continue;
            }
            for (String p : parameter.split("&")) {
                String[] pair = p.split("=");
                if (pair[0].equals("lang") && pair[1].equals(lang)) {
                    states.add(state);
                }
            }
        }
        return states;
    }

    public static State getState(String key) {
        for (State state : stateList) {
            if (state.key.equals(key)) {
                return state;
            }
        }
        return null;
    }

    public static List<State> getStateList() {
        return stateList;
    }

    /**
     * Generate a new State object with unique key and date
     * @return State object
     */
    public static State generateState() {
        DateTime dateTime = new DateTime();
        String time = dateTime.getMonthOfYear() + "_"
                + dateTime.getDayOfMonth() + "_"
                + dateTime.getYear() + "_"
                + dateTime.getHourOfDay() + "_"
                + dateTime.getMinuteOfHour() + "_"
                + dateTime.getSecondOfMinute();

        String key = null;
        while (true) {
            key = RandomStringUtils.randomAlphanumeric(8);

            boolean same = false;
            for (State state : stateList) {
                if (state.key.equals(key)) {
                    same = true;
                    break;
                }
            }

            if (!same) {
                break;
            }
        }

        State state = new State();
        state.key   = key;
        state.time  = time;
        stateList.add(state);
        update();
        return state;
    }

    /**
     * Clone a state with new key and date
     * @param key key of the state to be cloned
     * @return
     */
    public static State cloneState(String key) {
        if (hasKey(key)) {
            State state = generateState();

            state.setPairFile(getPairFile(key));
            state.setProcFile(getProcFile(key));
            state.setModelFile(getModelFile(key));
            state.setParameter(getParameter(key));

            return state;
        } else {
            return null;
        }
    }

    public static class State {
        String key;
        String pairFile;
        String procFile;
        String modelFile;
        String time;
        String parameter;
        String msg;

        public State() {

        }

        public State(String line) {
            this(new JSONObject(line));
        }

        public State(JSONObject json) {
            if (json.has("key")) {
                key = json.getString("key");
            } else {
                return;
            }

            if (json.has("time")) {
                time = json.getString("time");
            } else {
                return;
            }

            if (json.has("pair_file")) {
                pairFile = json.getString("pair_file");
            }

            if (json.has("proc_file")) {
                procFile = json.getString("proc_file");
            }

            if (json.has("model_file")) {
                modelFile = json.getString("model_file");
            }

            if (json.has("parameter")) {
                parameter = json.getString("parameter");
            }

            if (json.has("msg")) {
                msg = json.getString("msg");
            }
        }

        public String getKey() {
            return key;
        }

        public String getPairFile() {
            return pairFile;
        }

        public String getProcFile() {
            return procFile;
        }

        public String getModelFile() {
            return modelFile;
        }

        public String getParameter() {
            return parameter;
        }

        public String getTime() {
            return time;
        }

        public String getMsg() {
            return msg;
        }

        public void setPairFile(String pairFile) {
            this.pairFile = pairFile;
        }

        public void setProcFile(String procFile) {
            this.procFile = procFile;
        }

        public void setModelFile(String modelFile) {
            this.modelFile = modelFile;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();

            if (key != null && key.length() > 0) {
                json.put("key", key);
            }

            if (time != null && time.length() > 0) {
                json.put("time", time);
            }

            if (pairFile != null && pairFile.length() > 0) {
                json.put("pair_file", pairFile);
            }

            if (procFile != null && procFile.length() > 0) {
                json.put("proc_file", procFile);
            }

            if (modelFile != null && modelFile.length() > 0) {
                json.put("model_file", modelFile);
            }

            if (parameter != null && parameter.length() > 0) {
                json.put("parameter", parameter);
            }

            if (msg != null && msg.length() > 0) {
                json.put("msg", msg);
            }

            return json;
        }
    }
}
