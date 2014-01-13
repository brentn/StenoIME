package com.brentandjody.stenoime;


import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Created by brentn on 04/01/14.
 */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView view = (TextView) findViewById(R.id.aboutText);
        String formattedText = getString(R.string.about_text);
        Spanned result = Html.fromHtml(formattedText);
        view.setText(result);
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }
}