package com.skinterface.demo.android;

//import com.google.gwt.i18n.client.DateTimeFormat;
//import com.google.gwt.i18n.client.TimeZone;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Words for Rapid Serial Visual Presentation
 */
public final class RsvpWords {

    public static final int DEFAULT_WEIGHT = 6;
    public static final char WORD_JOINER = ' '; // '·'
    public static final char WORD_SHY = '\u00AD';

    // Just-read Title
    public static final int JR_TITLE   = 1;
    // Just-read Intro
    public static final int JR_INTRO   = 2;
    // Just-read Article
    public static final int JR_ARTICLE = 4;
    // Just-read Value
    public static final int JR_VALUE   = 8;
    // Just-read Warning
    public static final int JR_WARNING = 16;

    public static class Word {
        final char icon;
        final String word;
        final int weight;
        public Word(char icon, String word, int weight) {
            this.icon = icon;
            this.word = word;
            this.weight = weight;
        }
    }

    public static class Part {
        final String title;
        final String text;
        public Part(String title, String word) {
            this.title = title;
            this.text = word;
        }
    }

    private final ArrayList<Word> words = new ArrayList<>();
    private final ArrayList<Part> parts = new ArrayList<>();
    private int justRead;

    public RsvpWords() {
    }

    public Part[] getText() {
        return parts.toArray(new Part[parts.size()]);
    }

    public int getJustRead() {
        return justRead;
    }

    public int size() {
        return words.size();
    }

    public Word[] getArray() {
        return words.toArray(new Word[words.size()]);
    }

    public Word get(int i) {
        return words.get(i);
    }

    public RsvpWords addPause() {
        words.add(new Word('\0', "", DEFAULT_WEIGHT));
        return this;
    }
    public RsvpWords addTitleWords(SEntity entity) {
        justRead |= JR_TITLE;
        return addWords("Title", '§', entity == null ? null : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addIntroWords(SEntity entity) {
        justRead |= JR_INTRO;
        return addWords("Intro", '¶', entity == null ? null : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addArticleWords(SEntity entity) {
        justRead |= JR_ARTICLE;
        return addWords("Article", '\0', entity == null ? null : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addMenuWords(int idx, SEntity entity) {
        return addWords("Menu "+Integer.toString(1+idx), (char)('1'+idx), entity == null ? "***" : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addWarning(String text) {
        justRead |= JR_WARNING;
        return addWords("Warning", '⚠', text, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addValueWords(SSect sect) {
        if (sect == null || !sect.isValue)
            return this;
        SEntity entity = sect.entity;
        justRead |= JR_VALUE;
        char ch = '#';
        if ("date".equals(entity.media) || "datetime".equals(entity.media))
            ch = '⏲';
        else if ("geolocation".equals(entity.media))
            ch = '⚑';
        else if ("text".equals(entity.media) || "string".equals(entity.media))
            ch = 'ω';
        else if ("int".equals(entity.media) || "real".equals(entity.media))
            ch = 'ω';
        String val = makeValueText(entity);
        if (val != null) {
            if (val.isEmpty()) {
                addWords("Value", ch, "∅", 3*DEFAULT_WEIGHT);
            } else {
                addWords("Value", ch, val, 3*DEFAULT_WEIGHT);
            }
        }
        return this;
    }
    private RsvpWords addWords(String title, char icon, String text, int minWeight) {
        if (text == null)
            text = "";
        else
            text = text.trim();
        parts.add(new Part(title, text));
        if (text.isEmpty()) {
            words.add(new Word(icon, "∅", minWeight));
        } else {
            ArrayList<String> split = splitText(text);
            if (split.size() == 1) {
                words.add(new Word(icon, split.get(0), minWeight));
                return this;
            }
            int baseWeight = DEFAULT_WEIGHT;
            if (split.size() * DEFAULT_WEIGHT < minWeight)
                baseWeight = minWeight / split.size();
            for (String w : split)
                words.add(new Word(icon, w, baseWeight + addWeight(w)));
        }
        return this;
    }
    private int addWeight(String word) {
        int weight = 0;
        char end = word.charAt(word.length() - 1);
        if (word.length() >= 10) {
            weight += 2;
            if (word.length() >= 14)
                weight += 2;
            switch (end) {
                case ',':
                case ':':
                    weight += 2; break;
                case '.':
                case '!':
                case '?':
                case ';':
                    weight += 4; break;
            }
        } else {
            switch (end) {
                case ',':
                case ':':
                    weight += 4; break;
                case '.':
                case '!':
                case '?':
                case ';':
                    weight += 6; break;
            }
        }
        if (weight == 0 && word.indexOf(WORD_JOINER) > 0)
            weight += 2;
        return weight;
    }

    private String makeValueText(SEntity value) {
        if (value == null)
            return null;
        if (value.data == null || value.data.isEmpty())
            return "";
        switch (value.media) {
            case "geolocation":
                try {
                    JSONObject jobj = new JSONObject(value.data);
                    if (jobj.has("address"))
                        return jobj.getString("address");
                } catch (Exception e) {
                    addWarning("Bad location value");
                }
                return null;
            case "date":
            case "datetime":
//                try {
//                    String s = value.data;
//                    DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.ISO_8601);
//                    Date date = dtf.parse(s);
//                    Date dtz1 = dtf.parse("1970-01-01T00:00:00.000" + s.substring(s.length() - 6));
//                    int offs = (int) dtz1.getTime();
//                    TimeZone tz = com.google.gwt.i18n.client.TimeZone.createTimeZone(offs / 1000 / 60);
//                    dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_FULL);
//                    return dtf.format(date, tz);
//                } catch (Exception e) {
//                    addWarning("Bad date value");
//                }
                return null;
            case "text":
            case "int":
            case "real":
                return value.data;
        }
        return null;
    }

    private ArrayList<String> splitText(String text) {
        ArrayList<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(text.split("\\s+")));
        // join or short words
        for (int i=0; i < list.size()-1; ++i) {
            String w0 = list.get(i);
            String w1 = list.get(i+1);
            if (w0.length() == 1 && w1.length() <= 5) {
                list.set(i, w0 + WORD_JOINER + w1);
                list.remove(i+1);
                continue;
            }
            if (w0.length() == 2 && w1.length() <= 4) {
                list.set(i, w0 + WORD_JOINER + w1);
                list.remove(i+1);
                continue;
            }
            if (w1.length() <= 10)
                continue;
            String w2;
            int p = w1.indexOf('-');
            if (p > 0) {
                w2 = w1.substring(p+1);
                w1 = w1.substring(0, p+1);
            }
            else if ((p = w1.indexOf(WORD_SHY)) > 0) {
                w2 = w1.substring(p+1);
                w1 = w1.substring(0, p)+'-';
            }
            else
                continue;
            if (w0.length() == 1 && w1.length() <= 6) {
                list.set(i, w0 + WORD_JOINER + w1);
                list.set(i+1, w2);
                continue;
            }
            if (w0.length() == 2 && w1.length() <= 5) {
                list.set(i, w0 + WORD_JOINER + w1);
                list.set(i+1, w2);
                continue;
            }
            list.set(i+1, w1);
            list.add(i+2, w2);
        }
        return list;
    }
}
