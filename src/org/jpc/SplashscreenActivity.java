package org.jpc;

import android.webkit.WebView;
import java.io.*;
import android.view.*;
import android.app.*;
import android.os.*;
import android.app.*;
import android.content.*;
import android.widget.*;
import android.view.animation.*;
import android.view.animation.Animation.*;

public class SplashscreenActivity extends Activity
{
    View v;
    AnimationSet anim;

    public void onCreate(Bundle savedInstanceState)
    {
        System.out.println("OnCreate SplashscreenActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);
        v = (ImageView) findViewById(R.id.splash);
        anim = new AnimationSet(true);
        AlphaAnimation fadein = new AlphaAnimation(0.0F, 1.0F);
        fadein.setDuration(2000); 
        AlphaAnimation fadeout = new AlphaAnimation(1.0F, 0.0F);
        fadeout.setDuration(2000); 
        fadeout.setStartOffset(3000+fadein.getStartOffset());
        anim.addAnimation(fadein);
        anim.addAnimation(fadeout);
        anim.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    // TODO Auto-generated method stub
                }
                
                public void onAnimationRepeat(Animation animation) {
                    // TODO Auto-generated method stub
                }
                
                public void onAnimationEnd(Animation animation) {
                    startConSoul();
                }
            });
    }

    public void onResume()
    {
        super.onResume();
        v.startAnimation(anim);
    }

    public void startConSoul()
    {
        v.setVisibility(View.INVISIBLE);
        Intent i = new Intent(this, ConSoul.class);
        startActivity(i);
        finish();
    }
}
