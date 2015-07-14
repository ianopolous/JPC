package org.jpc;

import java.io.*;
import java.util.zip.*;
import android.view.*;
import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import android.graphics.*;
import android.widget.AdapterView.*;
import android.content.res.*;

public class DetailsActivity extends Activity
{
    public static final int REQUEST_CODE = 100;
    private String vm;
    private ImageView screen;

    public void onCreate(Bundle savedInstanceState)
    {
        System.out.println("OnCreate DetailsActivity");
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            setContentView(R.layout.details_landscape);
        else
            setContentView(R.layout.details_portrait);
        Intent intent = getIntent();
        vm = intent.getStringExtra("vm");
        
        TextView title = (TextView) findViewById(R.id.title);
        title.setText(getTitle(vm));
        screen = (ImageView) findViewById(R.id.screen);
        screen.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        screen.setImageBitmap(ImageAdapter.addBorder(BitmapFactory.decodeFile(getExternalFilesDir(null)+"/"+vm+"/screenshot.jpg"), getResources().getColor(R.color.theme)));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        System.out.println("Returned from emulator");
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE){
            //Bundle b = data.getExtras();
            //byte[] ss = b.getByteArray("snapshot");
            //saveSnapshot(ss, b.getInt("width"), b.getInt("height"));
            // update thumbnail
            screen.setImageBitmap(ImageAdapter.addBorder(BitmapFactory.decodeFile(getExternalFilesDir(null)+"/"+vm+"/screenshot.jpg"), getResources().getColor(R.color.theme)));
        }
    }

    public static byte[] getSnapshot(File base, String vm)
    {
        return ConSoul.getFileData(base, vm+"/snapshot.ss");
    }

    public static byte[] getVGA(File base, String vm)
    {
        return ConSoul.getFileData(base, vm+"/vga.ss");
    }

    public static void saveVGA(File base, String vm, byte[] vga)
    {
        if (vm == null)
            throw new IllegalStateException("Trying to save vga with null vm name");
        ConSoul.writeDataToFile(base, vm, "vga.ss", vga);
    }

    public static void saveSnapshot(File base, String vm, byte[] ss, int[] screen, int width, int height)
    {
        if (vm == null)
            throw new IllegalStateException("Trying to save snapshot with null vm name");
        // save snapshot to vm folder
        ConSoul.writeDataToFile(base, vm, "snapshot.ss", ss);

        // save screen to folder as well
        // convert from INT_RGB to INT_ARGB
        int alpha = 0xFF << 24;
        for (int i=0; i < screen.length; i++)
            screen[i] |= alpha;
        Bitmap result = Bitmap.createBitmap(screen, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        ConSoul.writeDataToFile(base, vm, "screenshot.jpg", stream.toByteArray());
    }

    private String getTitle(String vm)
    {
        return ConSoul.getTextOfFile(getExternalFilesDir(null), vm, "title.txt");
    }

    private String getConfig(String vm)
    {
        return ConSoul.getTextOfFile(getExternalFilesDir(null), vm, "config.txt");
    }

    public void launch(View v)
    {
        Intent i = new Intent(this, JPC.class);
        i.putExtra("args", getConfig(vm));
        i.putExtra("vm", vm);
        startActivityForResult(i, REQUEST_CODE);
    }
}
