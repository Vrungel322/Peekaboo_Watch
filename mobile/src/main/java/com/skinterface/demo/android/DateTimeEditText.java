package com.skinterface.demo.android;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateTimeEditText extends EditText {

    private static Calendar cal = Calendar.getInstance();

    private String current;
    private boolean silent;

    private OnDateTimeCompleteListener listener;

    public interface OnDateTimeCompleteListener {
        void onDateTimeComplete(DateTimeEditText dt, String str);
    }

    public DateTimeEditText(Context context) {
        super(context);
        init();
    }

    public DateTimeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DateTimeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_NUMBER);
        setTypeface(Typeface.MONOSPACE);
    }

    public OnDateTimeCompleteListener getDateTimeCompleteListener() {
        return listener;
    }

    public void setDateTimeCompleteListener(OnDateTimeCompleteListener listener) {
        this.listener = listener;
    }

    public void setDateString(String date) {
        if (date == null || date.isEmpty()) {
            setText("");
            return;
        }
        silent = true;
        try {
            setText(date);
        } catch (Exception e) {}
        silent = false;
    }

    private String substr(String str, int beg, int sz) {
        if (beg >= str.length())
            return "";
        if (beg+sz > str.length())
            sz = str.length()-beg;
        return str.substring(beg, beg+sz);
    }

    private void append(SpannableStringBuilder sb, String str, String templ) {
        sb.append(str);
        if (str.length() < templ.length()) {
            int pos = sb.length();
            int add = templ.length() - str.length();
            sb.append(templ.substring(str.length(), templ.length()));
            Object span = new ForegroundColorSpan(getCurrentHintTextColor());
            sb.setSpan(span, pos, pos+add, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String text = s.toString();
        if (text.equals(current))
            return;
        String clean = text.replaceAll("[^\\d]", "");
        if (count == 0 && !clean.isEmpty()) {
            // backspace was pressed?
            if (start == 13 || start == 10 || start == 7 || start == 4)
                clean = clean.substring(0, clean.length()-1);
        }

        String YY = substr(clean, 0, 4);
        String MN = substr(clean, 4, 2);
        String DD = substr(clean, 6, 2);
        String HH = substr(clean, 8, 2);
        String MI = substr(clean,10, 2);

        int yy = 0, mn = 0, dd = 0, hh=0, mi=0;
        if (YY.length() == 4)
            yy = Integer.parseInt(YY);
        if (MN.length() == 2) {
            mn = Integer.parseInt(MN);
            if (mn < 0 ) { mn = 1; MN = "01"; }
            if (mn > 12) { mn = 12; MN = "12"; }
        }
        if (DD.length() == 2) {
            dd = Integer.parseInt(DD);
            if (dd > 31) { dd = 31; DD = "31"; }
        }
        if (HH.length() == 2) {
            hh = Integer.parseInt(HH);
            if (hh > 23) { hh = 23; HH = "23"; }
        }
        if (MI.length() == 2) {
            mi = Integer.parseInt(MI);
            if (mi > 59) { mi = 59; MI = "59"; }
        }

        if (DD.length() == 2) {
            //This part makes sure that when we finish entering numbers
            //the date is correct, fixing it otherwise
            cal.set(Calendar.MONTH, mn-1);
            cal.set(Calendar.YEAR, yy);
            // ^ first set year for the line below to work correctly
            //with leap years - otherwise, date e.g. 29/02/2012
            //would be automatically corrected to 28/02/2012
            int md = cal.getActualMaximum(Calendar.DATE);
            if (dd > md) {
                dd = md;
                DD = Integer.toString(dd);
            }
        }

//        sel = sel < 0 ? 0 : sel;
        SpannableStringBuilder sb = new SpannableStringBuilder();
        append(sb, YY, "yyyy");
        sb.append('-');
        append(sb, MN, "mm");
        sb.append('-');
        append(sb, DD, "dd");
        sb.append(' ');
        append(sb, HH, "hh");
        sb.append(':');
        append(sb, MI, "mm");
        current = sb.toString();
        this.setText(sb);
        int pos = 0;
        if (YY.length() < 4)
            pos = YY.length();
        else if (MN.length() < 2)
            pos = 5 + MN.length();
        else if (DD.length() < 2)
            pos = 8 + DD.length();
        else if (HH.length() < 2)
            pos = 11 + HH.length();
        else
            pos = 14 + MI.length();
        this.setSelection(pos);
        if (pos == 16 && !silent && listener != null) {
            listener.onDateTimeComplete(this, current.replace(' ', 'T'));
        }
    }
}
