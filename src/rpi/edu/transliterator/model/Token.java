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

import java.util.HashMap;
import java.util.Map;

public class Token {
    private int startChar;
    private int endChar;
    private String text;
    private Map<String, Object> attributeMap;

    /* Constructors */
    public Token() {
        attributeMap = new HashMap<>();
    }

    public Token(String text) {
        this();
        this.text = text;
    }

    public Token(String text, int startChar, int endChar) {
        this();
        this.text = text;
        this.startChar = startChar;
        this.endChar   = endChar;
    }


    /* Mutators and accessors */
    public void text(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public void setStartChar(int startChar) {
        this.startChar = startChar;
    }

    public int getStartChar() {
        return startChar;
    }

    public void setEndChar(int endChar) {
        this.endChar = endChar;
    }

    public int getEndChar() {
        return endChar;
    }


    /* Attribute map operations */
    public void addAttribute(String key, Object value) {
        attributeMap.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributeMap.get(key);
    }

    public void removeAttribute(String key) {
        if (attributeMap.containsKey(key)) {
            attributeMap.remove(key);
        }
    }

    public boolean hasAttribute(String key) {
        return attributeMap.containsKey(key);
    }

    public Map<String, Object> getAttributeMap() {
        return attributeMap;
    }
}