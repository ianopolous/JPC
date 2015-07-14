package org.jpc;

import android.webkit.WebView;
import java.io.*;
import android.view.*;
import android.app.*;

public class HtmlView extends WebView
{
    private Activity jpc;

    public HtmlView(String htmlRes, Activity jpc)
    {
        super(jpc);
        this.jpc = jpc;
        loadUrl("file:///android_asset/"+htmlRes);
        if (!JPC.IS_OUYA)
            setInitialScale(120);
    }

    public void hide()
    {
        jpc.finish();
    }

    public boolean onKeyDown (int keyCode, KeyEvent event)
    {
        hide();
        return true;
    }
}
