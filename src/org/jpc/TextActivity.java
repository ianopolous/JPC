package org.jpc;

import android.webkit.WebView;
import java.io.*;
import android.view.*;
import android.app.*;
import android.os.*;
import android.app.*;
import android.content.*;
import android.widget.*;

public class TextActivity extends Activity
{

    public void onCreate(Bundle savedInstanceState)
    {
        System.out.println("OnCreate TextActivity");
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String source = intent.getStringExtra("text");
        HtmlView html = new HtmlView(source, this);
        setContentView(html);
    }
}
