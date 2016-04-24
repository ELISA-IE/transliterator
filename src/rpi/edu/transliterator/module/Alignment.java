package rpi.edu.transliterator.module;

import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

// Ying_2_31_2016_12_28_16.kan.x3y3c0.001fjoint.model
public class Alignment {
    public static boolean toNewsFormat(String key, PropertiesConfiguration config) {
        String pairFile = States.getPairFile(key);
        String time = States.getTime(key);
        if (pairFile == null || time == null) {
            return false;
        }

        String line;
        Set<String> pairSet = new HashSet<>();
        BufferedReader br;
        BufferedWriter bw;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream("data/pair/" + pairFile), "utf-8"));

            while ((line = br.readLine()) != null) {
                String[] pair = line.split("\t");

                if (pair.length != 2) {
                    continue;
                }

                String[] source = pair[0].split(" ");
                String[] target = pair[1].split(" ");

                if (source.length == target.length) {
                    for (int i = 0, l = source.length; i < l; i++) {
                        String tokenPair = source[i] + '\t' + target[i];
                        pairSet.add(tokenPair);
                    }
                }
            }
            br.close();

            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("data/proc/" + time + ".proc.txt"), "utf-8"));
            for (String pair : pairSet) {
                String outputLine = "";
                // add space after each character
                for (char c : pair.toCharArray()) {
                    outputLine += c + " ";
                }
                outputLine = outputLine.replace(" \t ", "\t").trim();

                bw.write(outputLine);
                bw.newLine();
            }
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        States.setProcFile(key, time + ".proc.txt");
        return true;
    }

    public static Process m2mAlign(String key, PropertiesConfiguration config) {
        if (!States.hasKey(key)) {
            return null;
        }

        String time = States.getTime(key);
        String procFile  = States.getProcFile(key);
        String parameter  = States.getParameter(key);
        if (time == null || procFile == null) {
            return null;
        }

        // get parameters from config
        String name = config.getString("name", "Unknown");
        int maxX = Integer.parseInt(config.getString("maxx", "2"));
        int maxY = Integer.parseInt(config.getString("maxy", "2"));
        double cutoff = Double.parseDouble(config.getString("cutoff", "0.001"));
        String maxFn = config.getString("maxfn", "joint");
        String modelFile = States.getFilename(key); // name + "_" + time + ".x" + maxX + "y" + maxY
//                + "." + Double.toString(cutoff).replace(".", "_")
//                + "." + maxFn + ".model.txt";

        String cmd = "lib/m2m-aligner/m2m-aligner";
        cmd += " --maxX " + maxX + " --maxY " + maxY + " --maxFn " + maxFn + " --cutoff " + cutoff;
        cmd += " -i " + "data/proc/" + procFile;
        cmd += " -o " + "data/model/" + modelFile;

        if (parameter == null || parameter.length() == 0) {
            parameter = "maxx=" + maxX + "&maxy=" + maxY + "&maxfn=" + maxFn
                    + "&cutoff=" + cutoff + "&name=" + name;
        } else {
            parameter += "&maxx=" + maxX + "&maxy=" + maxY + "&maxfn=" + maxFn
                    + "&cutoff=" + cutoff + "&name=" + name;
        }

        Runtime rt = Runtime.getRuntime();
        try {
            return rt.exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        boolean tnfResult = toNewsFormat("rFdpmNpO", null);
        System.out.println(tnfResult);
    }
}
