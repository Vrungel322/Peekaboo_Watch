package com.skinterface.demo.android;

import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class Fmt {

    private Fmt() {}

    private static void addQuotedChar(StringBuilder sb, char ch) {
        if (ch == '\"') { sb.append("&quot;"); return; }
        if (ch == '&') { sb.append("&amp;"); return; }
        if (ch == '<') { sb.append("&lt;"); return; }
        if (ch == '>') { sb.append("&gt;"); return; }
        if (ch == '\u00A0') { sb.append("&nbsp;"); return; }
        sb.append(ch);
    }

    private static void addQuotedString(StringBuilder sb, String str) {
        for (int i=0; i < str.length(); ++i)
            addQuotedChar(sb, str.charAt(i));
    }

    private static CharSequence trimTrailingWhitespace(CharSequence source) {
        if (source == null)
            return null;
        int i = source.length();
        while(--i >= 0 && Character.isWhitespace(source.charAt(i)))
            ;
        if (i+1 == source.length())
            return source;
        return source.subSequence(0, i+1);
    }

    public static CharSequence toSpannedText(String text) {
        if (text == null || text.isEmpty())
            return text;
        try {
            StringBuilder sb = new StringBuilder();
            // split text into paragraphs: empty lines or ¶ signs
            boolean in_para = false;
            BufferedReader r = new BufferedReader(new StringReader(text));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    if (in_para) {
                        in_para = false;
                        //sb.append("</p>");
                    }
                    continue;
                }
                if (line.indexOf('¶') < 0) {
                    if (!in_para) {
                        sb.append("<p>");
                        in_para = true;
                        addQuotedString(sb, line);
                    } else {
                        sb.append('\n');
                        addQuotedString(sb, line);
                    }
                    continue;
                }
                for (int i=0; i < line.length(); ++i) {
                    char ch = line.charAt(i);
                    if (ch == '¶') {
                        if (!in_para) {
                            sb.append("<p>&nbsp"); // "<p>&nbsp</p>"
                        } else {
                            //sb.append("</p>");
                            in_para = false;
                        }
                    } else {
                        if (!in_para) {
                            sb.append("<p>");
                            in_para = true;
                        }
                        addQuotedChar(sb, ch);
                    }
                }
            }
            //if (in_para)
            //    sb.append("</p>");
            String html = sb.toString();
            CharSequence sp = trimTrailingWhitespace(Html.fromHtml(html));
            return sp;
        } catch (IOException e) {
            return text;
        }
    }
}
