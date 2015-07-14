package org.jpc;

import android.widget.*;
import android.content.*;
import android.view.*;
import android.graphics.*;
import java.util.*;
import java.io.*;

public class ImageAdapter extends BaseAdapter
{
    private Context mContext;
    String[] vms;
    Bitmap[] screens;

    public ImageAdapter(Context c)
    {
        mContext = c;
        vms = getVMList(c);
    }

    public void regenerateIndex(Context c)
    {
        vms = getVMList(c);
    }

    private String[] getVMList(Context c)
    {
        List<String> vmdirs = new ArrayList();
        List<Bitmap> bms = new ArrayList();
        File base = c.getExternalFilesDir(null);
        for (File f: base.listFiles())
            if (f.isDirectory())
            {
                vmdirs.add(f.getName());
                bms.add(addBorder(BitmapFactory.decodeFile(f.getAbsolutePath()+"/screenshot.jpg"), c.getResources().getColor(R.color.theme)));
            }
        screens = bms.toArray(new Bitmap[0]);
        return vmdirs.toArray(new String[0]);
    }

    public static Bitmap addBorder(Bitmap in, int borderColour)
    {
        int border = 10;
        RectF targetRect = new RectF(border, border, border + in.getWidth(), border + in.getHeight());
        Bitmap dest = Bitmap.createBitmap(in.getWidth()+2*border, in.getHeight()+2*border, in.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawColor(borderColour);
        canvas.drawBitmap(in, null, targetRect, null);
        return dest;
    }

    public int getCount() {
        return vms.length;
    }

    public Object getItem(int position) {
        return vms[position];
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View thumbView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            thumbView = LayoutInflater.from(mContext).inflate(R.layout.thumbnail, null);
        } else {
            thumbView = convertView;
        }
        ((ImageView)thumbView.findViewById(R.id.screen)).setImageBitmap(screens[position]);
        ((TextView)thumbView.findViewById(R.id.name)).setText(ConSoul.getTextOfFile(mContext.getExternalFilesDir(null), vms[position], "title.txt"));
        return thumbView;
    }
}