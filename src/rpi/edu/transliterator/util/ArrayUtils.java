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

package rpi.edu.transliterator.util;

import java.lang.reflect.Array;

public class ArrayUtils {

    public static final  <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public static final <T> T[][] concatenate(T[][] a, T[][] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[][] c = (T[][]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public static final <T> T[] append(T[] a, T b) {
        int aLen = a.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + 1);
        System.arraycopy(a, 0, c, 0, aLen);
        c[aLen] = b;

        return c;
    }

    public static final <T> T[][] append(T[][] a, T[] b) {
        int aLen = a.length;
        // int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[][] c = (T[][]) Array.newInstance(a.getClass().getComponentType(), aLen + 1);
        System.arraycopy(a, 0, c, 0, aLen);
        c[aLen] = b;
        // c[aLen] = (T[]) Array.newInstance(b.getClass().getComponentType(), bLen);
        // System.arraycopy(b, 0, c[aLen], 0, bLen);

        return c;
    }

    public static final <T> T[] prepend(T[] a, T b) {
        int aLen = a.length;

        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + 1);
        System.arraycopy(a, 0, c, 1, aLen);
        c[0] = b;

        return c;
    }

    public static final <T> T[][] prepend(T[][] a, T[] b) {
        int aLen = a.length;

        T[][] c = (T[][]) Array.newInstance(a.getClass().getComponentType(), aLen + 1);
        System.arraycopy(a, 0, c, 1, aLen);
        c[0] = b;

        return c;
    }

    public static void main(String[] args) {
        double[][] a = {{0.1, 0.2}, {0.3, 0.4}};
        double[][] b = {{0.7, 0.8}, {0.9, 1.0}};
        double[][] c = concatenate(a, b);
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[0].length; j++) {
                System.out.print(c[i][j] + " ");
            }
            System.out.println();
        }
    }
}
