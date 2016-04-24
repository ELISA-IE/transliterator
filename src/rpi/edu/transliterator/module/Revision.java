/*
 * Joint Transliteration and Entity Linking
 * Copyright (C) 2016 Ying Lin, Blender Lab
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rpi.edu.transliterator.module;

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import rpi.edu.transliterator.model.Pair;
import rpi.edu.transliterator.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class Revision {
    private static NormalizedLevenshtein normLev = new NormalizedLevenshtein();

    public static Pair<String, Double> revise(String trans, String link) {
        String[] transSeg  = trans.split("[ -]");
        String[] linkSeg   = link.split("[ -]");
        String[] transNorm = StringUtils.normalizeString(trans.split("[ -]"));
        String[] linkNorm  = StringUtils.normalizeString(link.split("[ -]"));

        int transNum = transNorm.length;
        int linkNum  = linkNorm.length;
        if (transNum == 1 && linkNum == 1) {
            return new Pair<>(link, normLev.similarity(transNorm[0], linkNorm[0]));
        }

        // not combine
        double notCombSim = 0.0;
        int[][] notCombPairs = new int[(transNum < linkNum)? transNum : linkNum][2];
        double[] notCombSims  = new double[(transNum < linkNum)? transNum : linkNum];
        if (transNum <= linkNum) {
            double factor = 1.0 / transNum;

            for (int i = 0; i < transNum; i++) {
                double maxSim = -1;
                int maxJ = -1;
                for (int j = 0; j < linkNum; j++) {
                    double sim = normLev.similarity(transNorm[i], linkNorm[j]);
                    if (sim > maxSim) {
                        maxJ   = j;
                        maxSim = sim;
                    }
                }
                notCombSim += factor * maxSim;
                notCombPairs[i][0] = i;
                notCombPairs[i][1] = maxJ;
                notCombSims[i] = maxSim;
            }
        } else {
            double factor = 1.0 / linkNum;

            for (int i = 0; i < linkNum; i++) {
                double maxSim = -1;
                int maxJ = -1;
                for (int j = 0; j < transNum; j++) {
                    double sim = normLev.similarity(transNorm[j], linkNorm[i]);
                    if (sim > maxSim) {
                        maxJ   = j;
                        maxSim = sim;
                    }
                }
                notCombSim += factor * maxSim;
                notCombPairs[i][0] = maxJ;
                notCombPairs[i][1] = i;
                notCombSims[i] = maxSim;
            }
        }

        // combine
        double combSim = 0.0;
        int[][] combPairs = new int[(transNum < linkNum)? transNum : linkNum][3];
        double[] combSims  = new double[(transNum < linkNum)? transNum : linkNum];
        if (transNum <= linkNum) {
            double factor = 1.0 / transNum;
            for (int i = 0; i < transNum; i++) {
                double maxSim1 = -1;
                int maxJ1 = -1;
                // 1v1
                for (int j = 0; j < linkNum; j++) {
                    double sim = normLev.similarity(transNorm[i], linkNorm[j]);
                    if (sim > maxSim1) {
                        maxJ1   = j;
                        maxSim1 = sim;
                    }
                }
                // 1v2
                double maxSim2 = -1;
                int maxJ2 = -1;
                for (int j = 0; j < linkNum - 1; j++) {
                    double sim = normLev.similarity(transNorm[i], linkNorm[j] + linkNorm[j + 1]);
                    if (sim > maxSim2) {
                        maxJ2   = j;
                        maxSim2 = sim;
                    }
                }

                if (maxSim2 > maxSim1) {
                    combSim += factor * maxSim2;
                    combPairs[i][0] = i;
                    combPairs[i][1] = maxJ2;
                    combPairs[i][2] = 1;
                    combSims[i] = maxSim2;
                } else {
                    combSim += factor * maxSim1;
                    combPairs[i][0] = i;
                    combPairs[i][1] = maxJ1;
                    combPairs[i][2] = 0;
                    combSims[i] = maxSim1;
                }
            }
        } else {
            double factor = 1.0 / linkNum;
            for (int i = 0; i < linkNum; i++) {
                double maxSim1 = -1, maxSim2 = -1;
                int maxJ1 = -1, maxJ2 = -1;
                // 1v1
                for (int j = 0; j < transNum; j++) {
                    double sim = normLev.similarity(transNorm[j], linkNorm[i]);
                    if (sim > maxSim1) {
                        maxJ1   = j;
                        maxSim1 = sim;
                    }
                }

                // 2v1
                for (int j = 0; j < transNum - 1; j++) {
                    double sim = normLev.similarity(transNorm[j] + transNorm[j + 1], linkNorm[i]);
//                    double sim_rev = nlev.similarity(link_norm[i], trans_norm[j] + trans_norm[j + 1]);
                    if (sim > maxSim2) {
                        maxJ2   = j;
                        maxSim2 = sim;
                    }
                }

                if (maxSim2 > maxSim1) {
                    combSim += factor * maxSim2;
                    combPairs[i][0] = maxJ2;
                    combPairs[i][1] = i;
                    combPairs[i][2] = 1;
                    combSims[i] = maxSim2;
                } else {
                    combSim += factor * maxSim1;
                    combPairs[i][0] = maxJ1;
                    combPairs[i][1] = i;
                    combPairs[i][2] = 0;
                    combSims[i] = maxSim1;
                }
            }
        }

        // revise
        String revised = "";
        if (notCombSim >= combSim) {
            for (int i = 0; i < notCombSims.length; i++) {
                String revisedTrans = "";
                double maxSim = -1;
                for (int[] pair : notCombPairs) {
                    if (pair[0] == i && notCombSims[i] > maxSim) {
                        revisedTrans = linkSeg[pair[1]];
                        maxSim = notCombSims[i];
                    }
                }

                if (maxSim < 0) {
//                    continue;
                    if (revised.length() == 0) {
                        revised = transSeg[i];
                    } else {
                        revised += transSeg[i];
                    }
                    continue;
                }

                if (revised.length() == 0) {
                    revised = revisedTrans;
                } else {
                    revised += " " + revisedTrans;
                }
            }
        } else {
            if (transNum <= linkNum) {
                for (int i = 0; i < transNum; i++) {
                    String revisedTrans = "";
                    double maxSim = -1;
                    for (int[] pair : combPairs) {
                        if (pair[0] == i && combSims[i] > maxSim) {
                            maxSim = combSims[i];
                            if (pair[2] == 0) {
                                revisedTrans = linkSeg[pair[1]];
                            } else {
                                revisedTrans = linkSeg[pair[1]] + " " + linkSeg[pair[1] + 1];
                            }
                        }
                    }

                    if (maxSim < 0) {
                        if (revised.length() == 0) {
                            revised = transSeg[i];
                        } else {
                            revised += transSeg[i];
                        }
                        continue;
                    }

                    if (revised.length() == 0) {
                        revised = revisedTrans;
                    } else {
                        revised += " " + revisedTrans;
                    }
                }
            } else {
                for (int i = 0; i < transNum; i++) {
                    String revisedTrans = "";
                    double maxSim = -1;
                    boolean combined = false;
                    for (int[] pair  : combPairs) {
                        if (pair[0] == i && combSims[pair[1]] > maxSim) {
                            maxSim = combSims[pair[1]];
                            revisedTrans = linkSeg[pair[1]];
                            if (pair[2] == 1) {
                                combined = true;
                            } else {
                                combined = false;
                            }
                        }
                    }

                    if (maxSim < 0) {
                        if (revised.length() == 0) {
                            revised = transSeg[i];
                        } else {
                            revised += transSeg[i];
                        }
                        continue;
                    }

                    if (revised.length() == 0) {
                        revised = revisedTrans;
                    } else {
                        revised += " " + revisedTrans;
                    }

                    if (combined) {
                        i++;
                    }
                }
            }
        }

        double simMax = -1.0;
        String[] revisedNorm = StringUtils.normalizeString(revised.split(" "));
        for (int i = 0; i < transNum; i++) {
            for (int j = 0; j < revisedNorm.length; j++) {
                double sim = normLev.similarity(transNorm[i], revisedNorm[j]);
                if (sim > simMax) {
                    simMax = sim;
                }
            }
        }

        return new Pair<>(revised, simMax);
    }

    public static Pair<String, Double> updateTrans(String trans, String link) {
        String[] transSeg  = trans.split(" ");
        String[] linkSeg   = link.split(" ");
        String[] transNorm = StringUtils.normalizeString(trans.split(" "));
        String[] linkNorm  = StringUtils.normalizeString(link.split(" "));
        int transNum = transNorm.length;
        int linkNum  = linkNorm.length;

        // Calculate sim between each trans token and wiki token
        double[][] simMat = new double[transNum][linkNum];
        for (int i = 0; i < transNum; i++) {
            for (int j = 0; j < linkNum; j++) {
                simMat[i][j] = normLev.similarity(transNorm[i], linkNorm[j]);
            }
        }

        Set<Integer> matchedTrans = new HashSet<>(),
                matchedWiki = new HashSet<>();
        if (transNorm.length <= linkNorm.length) {
            int[] matchIndex = new int[transNum];
            for (int r = 0; r < transNum; r++) {
                double maxSim = -1;
                int iMax = -1, jMax = -1;
                for (int i = 0; i < transNum; i++) {
                    if (matchedTrans.contains(i)) {
                        continue;
                    }
                    for (int j = 0; j < linkNum; j++) {
                        if (matchedWiki.contains(j)) {
                            continue;
                        }
                        if (simMat[i][j] > maxSim) {
                            iMax = i;
                            jMax = j;
                            maxSim = simMat[i][j];
                        }
                    }
                }

                if (iMax != -1) {
                    matchIndex[iMax] = jMax;
                    matchedTrans.add(iMax);
                    matchedWiki.add(jMax);
                } else {
                    return new Pair<>(trans, 1.0);
                }
            }

            String[] updatedTrans = new String[transNum];
            for (int i = 0; i < transNum; i++) {
                updatedTrans[i] = linkSeg[matchIndex[i]];
            }

            double simMax = -1.0;
            String revised = "";
            String[] revised_norm = StringUtils.normalizeString(updatedTrans);
            for (int i = 0; i < transNum; i++) {
                if (i == 0) {
                    revised = updatedTrans[i];
                } else {
                    revised += " " + updatedTrans[i];
                }

                double sim = normLev.similarity(transNorm[i], revised_norm[i]);
                if (sim > simMax) {
                    simMax = sim;
                }
            }
            return new Pair<>(revised, simMax);
        } else {
            int[] matchIndex = new int[linkNum];
            for (int r = 0; r < linkNum; r++) {
                double maxSim = -1;
                int iMax = -1, jMax = -1;
                for (int i = 0; i < linkNum; i++){
                    if (matchedWiki.contains(i)) {
                        continue;
                    }
                    for (int j = 0; j < transNum; j++) {
                        if (matchedTrans.contains(j)) {
                            continue;
                        }
                        if (simMat[j][i] > maxSim) {
                            iMax = i;
                            jMax = j;
                            maxSim = simMat[j][i];
                        }
                    }
                }

                // System.out.println(trans[jMax] + "/" + wiki[iMax]);
                matchIndex[iMax] = jMax;
                matchedTrans.add(jMax);
                matchedWiki.add(iMax);
            }

            String[] updatedTrans = new String[transNum];
            for (int i = 0; i < transNum; i++) {
                updatedTrans[i] = "";
            }
            for (int i = 0; i < linkNum; i++) {
                updatedTrans[matchIndex[i]] = linkSeg[i];
            }

            double simMax = -1.0;
            String revised = "";
            String[] revisedNorm = StringUtils.normalizeString(updatedTrans);
            for (int i = 0; i < transNum; i++) {
                if (i == 0) {
                    revised = updatedTrans[i];
                } else {
                    revised += " " + updatedTrans[i];
                }

                double sim = normLev.similarity(transNorm[i], revisedNorm[i]);
                if (sim > simMax) {
                    simMax = sim;
                }
            }
            return new Pair<>(revised, simMax);
        }
    }
}
