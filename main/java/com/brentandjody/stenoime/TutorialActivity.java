package com.brentandjody.stenoime;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.brentandjody.stenoime.Input.SwipeActivity;

public class TutorialActivity extends SwipeActivity {

    private ViewFlipper pager;
    private float lastX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        pager = (ViewFlipper) findViewById(R.id.viewFlipper);

        Spanned formattedText = Html.fromHtml(getString(R.string.text_lesson1));
        ((TextView) findViewById(R.id.textView1)).setText(formattedText);
        ((TextView) findViewById(R.id.textView1)).setMovementMethod(LinkMovementMethod.getInstance());
        formattedText = Html.fromHtml(getString(R.string.text_lesson2));
        ((TextView) findViewById(R.id.textView2)).setText(formattedText);
        ((TextView) findViewById(R.id.textView2)).setMovementMethod(LinkMovementMethod.getInstance());
        formattedText = Html.fromHtml(getString(R.string.text_lesson3));
        ((TextView) findViewById(R.id.textView3)).setText(formattedText);
        ((TextView) findViewById(R.id.textView3)).setMovementMethod(LinkMovementMethod.getInstance());
        formattedText = Html.fromHtml(getString(R.string.text_lesson4));
        ((TextView) findViewById(R.id.textView4)).setText(formattedText);
        ((TextView) findViewById(R.id.textView4)).setMovementMethod(LinkMovementMethod.getInstance());
        formattedText = Html.fromHtml(getString(R.string.text_lesson5));
        ((TextView) findViewById(R.id.textView5)).setText(formattedText);
        ((TextView) findViewById(R.id.textView5)).setMovementMethod(LinkMovementMethod.getInstance());
        formattedText = Html.fromHtml(getString(R.string.text_lesson6));
        ((TextView) findViewById(R.id.textView6)).setText(formattedText);
        ((TextView) findViewById(R.id.textView6)).setMovementMethod(LinkMovementMethod.getInstance());
        formattedText = Html.fromHtml(getString(R.string.text_lesson7));
        ((TextView) findViewById(R.id.textView7)).setText(formattedText);
        ((TextView) findViewById(R.id.textView7)).setMovementMethod(LinkMovementMethod.getInstance());
    }


    @Override
    protected void previous() {
        if (pager.getDisplayedChild()==0)
            return;
        pager.setInAnimation(this, android.R.anim.slide_in_left);
        pager.setOutAnimation(this, android.R.anim.slide_out_right);
        pager.showPrevious();
    }

    @Override
    protected void next() {
        if (pager.getDisplayedChild() >= (pager.getChildCount()-1))
            return;
        pager.setInAnimation(this, R.anim.slide_in_right);
        pager.setOutAnimation(this, R.anim.slide_out_left);
        pager.showNext();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tutorial, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
