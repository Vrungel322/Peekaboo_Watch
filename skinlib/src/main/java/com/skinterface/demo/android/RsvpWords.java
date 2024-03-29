package com.skinterface.demo.android;

//import com.google.gwt.i18n.client.DateTimeFormat;
//import com.google.gwt.i18n.client.TimeZone;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Words for Rapid Serial Visual Presentation
 *
 * Word's in sequence have duration, measured in 1/60
 * of a second, to be presented on screens with 60fps.
 *
 * Default duration is 6 frames, 10 words per second,
 * 600 words per minute. This duration is for short words.
 * Very long words have extended duration, words
 * before commas and dots also lasts longer to mark the
 * pause of commas and dots.
 */
public final class RsvpWords {

    public static final int DEFAULT_WEIGHT = 6;
    public static final char WORD_JOINER = ' '; // '·'
    public static final char WORD_SHY = '\u00AD';

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
        final int type;
        final String text;
        public Part(int type, String word) {
            this.type = type;
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
        return addWords(Navigator.JR_TITLE, '\0', entity == null ? null : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addIntroWords(SEntity entity) {
        return addWords(Navigator.JR_INTRO, '\0', entity == null ? null : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addArticleWords(SEntity entity) {
        return addWords(Navigator.JR_ARTICLE, '\0', entity == null ? null : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addMenuWords(int idx, SEntity entity) {
        return addWords(Navigator.JR_MENU, (char)('1'+idx), entity == null ? "***" : entity.data, 3*DEFAULT_WEIGHT);
    }
    public RsvpWords addWarning(String text) {
        justRead |= Navigator.JR_WARNING;
        return addWords(Navigator.JR_WARNING, '⚠', text, 5*DEFAULT_WEIGHT);
    }
    public RsvpWords addValueWords(SSect sect) {
        if (sect == null || !sect.isValue)
            return this;
        SEntity entity = sect.entity;
        justRead |= Navigator.JR_VALUE;
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
                addWords(Navigator.JR_VALUE, ch, "∅", 5*DEFAULT_WEIGHT);
            } else {
                addWords(Navigator.JR_VALUE, ch, val, 30*DEFAULT_WEIGHT);
            }
        }
        return this;
    }
    private RsvpWords addWords(int jr, char icon, String text, int minWeight) {
        justRead |= jr;
        if (text == null)
            text = "";
        else
            text = text.trim();
        parts.add(new Part(jr, text));
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
        if (word.length() == 0)
            return 0;
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
                    weight += 2; break;
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
                    weight += 4; break;
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
                try {
                    return value.data;
                } catch (Exception e) {
                    addWarning("Bad date value");
                }
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
        for (int i=0; i < list.size(); ++i) {
            String w0 = list.get(i);
            if (w0.length() == 0)
                continue;
            switch (w0.charAt(w0.length()-1)) {
            case '.':case '!':case '?':
                list.add(i+1,"");
                list.add(i+2,"");
                continue;
            }
            if (i == list.size()-1)
                continue;
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
