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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Phrase {
    private String text;
    private List<Token> tokenList;
    private Map<String, Object> attributeMap;

    /* Constructors */
    public Phrase() {
        tokenList  = new ArrayList<>();
        attributeMap = new HashMap<>();
    }

    public Phrase(String text) {
        this();
        this.text  = text;
    }

    /* Mutators and accessors */
    public String text() {
        return text;
    }

    public void text(String text) {
        this.text = text;
    }

    /* Token list operations */
    public List<Token> getTokenList() {
        return tokenList;
    }

    public Token getToken(int i) {
        return tokenList.get(i);
    }

    public void addToken(Token token) {
        tokenList.add(token);
    }

    public void addTokens(Token... tokens) {
        for (Token token : tokens) {
            tokenList.add(token);
        }
    }

    public int getTokenNumber() {
        return tokenList.size();
    }

    public void setTokenList(List<Token> tokenList) {
        this.tokenList = tokenList;
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
