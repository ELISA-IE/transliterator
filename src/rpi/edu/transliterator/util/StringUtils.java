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

/**
 * limteng
 * Date: Feb 08, 2016.
 */
package rpi.edu.transliterator.util;

import java.text.Normalizer;

public class StringUtils {
    /**
     * Remove diacritical marks
     * @param string input string array
     * @return
     */
    public static final String[] normalizeString(String[] string) {
        int stringLen = string.length;
        String[] normalizedString = new String[stringLen];
        for (int i = 0; i < stringLen; i++) {
            normalizedString[i] = Normalizer.normalize(string[i], Normalizer.Form.NFD)
                    .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase();
        }
        return normalizedString;
    }

    /**
     * Remove diacritical marks
     * @param string input string
     * @return
     */
    public static final String normalizeString(String string) {
        String normalizedString = Normalizer.normalize(string, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase();
        return normalizedString;
    }
}
