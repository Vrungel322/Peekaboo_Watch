package com.skinterface.demo.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;

public class InputActivity extends Activity implements TextWatcher {

    private static Calendar cal = Calendar.getInstance();

    private String current;
    private boolean silent;

    private EditText editText;
    private boolean real_mode;
    private boolean date_mode;
    private char decimalSeparator = '.';

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_input);

        editText = (EditText)findViewById(R.id.input);
        editText.setSingleLine();
        editText.setHorizontallyScrolling(true);
        //editText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        DecimalFormat format = (DecimalFormat)NumberFormat.getInstance();
        decimalSeparator = format.getDecimalFormatSymbols().getDecimalSeparator();

        Intent intent = getIntent();
        if (intent.hasExtra("int")) {
            setDateString(intent.getStringExtra("int"));
        }
        else if (intent.hasExtra("real")) {
            real_mode = true;
            setDateString(intent.getStringExtra("real"));
        }
        if (intent.hasExtra("date")) {
            date_mode = true;
            editText.addTextChangedListener(this);
            setDateString(intent.getStringExtra("date"));
        }
        else if (intent.hasExtra("datetime")) {
            date_mode = true;
            editText.addTextChangedListener(this);
            setDateString(intent.getStringExtra("datetime"));
        }

    }

    private void setDateString(String date) {
        if (date == null || date.isEmpty()) {
            editText.setText("");
            return;
        }
        silent = true;
        try {
            editText.setText(date);
        } catch (Exception e) {}
        silent = false;
    }

    public void onKeyButtonClick(View view) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            String key = tv.getText().toString();
            String text = editText.getText().toString();
            int len = text.length();
            int s = editText.getSelectionStart();
            int e = editText.getSelectionEnd();
            if (key.equals("⌫")) {
                try {
                    if (s > 0)
                        editText.getText().delete(s-1, e);
                } catch (Exception thr) {}
                return;
            }
            if (key.equals("✔")) {
                if (date_mode)
                    text = text.replace(' ','T');
                if (text.length() > 0)
                    setResult(RESULT_OK, new Intent().putExtra("text", text));
                else
                    setResult(RESULT_CANCELED, new Intent());
                finish();
                return;
            }
            try {
                if (key.equals(".") || key.equals(",")) {
                    if (date_mode)
                        return;
                    key = new String(new char[]{decimalSeparator});
                    if (text.indexOf(key) >= 0)
                        return;
                }
                editText.getText().replace(s, e, key);
                if (!date_mode)
                    editText.setSelection(s + key.length());
            } catch (Exception thr) {}
        }
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
            Object span = new ForegroundColorSpan(editText.getCurrentHintTextColor());
            sb.setSpan(span, pos, pos+add, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
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
        editText.setText(sb);
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
        editText.setSelection(pos);
//        if (pos == 16 && !silent && listener != null) {
//            listener.onDateTimeComplete(this, current.replace(' ', 'T'));
//        }
    }
}
