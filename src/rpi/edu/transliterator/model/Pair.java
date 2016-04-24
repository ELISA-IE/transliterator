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
 * Date: Feb 01, 2016.
 */
package rpi.edu.transliterator.model;

public class Pair<K, V> {
    public K key;
    public V value;

    public Pair(K key, V value) {
        this.key   = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "(" + key.toString() + "," + value.toString() + ")";
    }
}
