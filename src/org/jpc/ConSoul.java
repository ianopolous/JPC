package org.jpc;

import java.io.*;
import android.view.*;
import android.app.*;
import android.os.*;
import android.app.*;
import android.content.*;
import android.widget.*;
import android.widget.AdapterView.*;

import tv.ouya.console.api.*;

public class ConSoul extends Activity
{
    public static boolean IS_OUYA;
    private ImageAdapter vms;
    private GridView gridview;

    public void onCreate(Bundle savedInstanceState)
    {
        System.out.println("OnCreate ConSoul");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.consoul);
        
        // determine if we are on an OUYA
        Intent intent = getIntent();
        IS_OUYA = intent.hasCategory("tv.ouya.intent.category.GAME");
        System.out.println("OUYA intents: " + intent);
        System.out.println("Is on OUYA, attempt 1: "+ IS_OUYA);
        if (!IS_OUYA)
        {
            IS_OUYA = OuyaFacade.getInstance().isRunningOnOUYAHardware();
            System.out.println("Is on OUYA, attempt 2: "+ IS_OUYA);
        }
        if (!IS_OUYA)
        {
            IS_OUYA = false;
            System.out.println("Is on OUYA, attempt 3: "+ IS_OUYA);
        }

        // ensure initial vm is installed
        ensureInitialVMSetup();

        gridview = (GridView) findViewById(R.id.ConSoul);
        vms = new ImageAdapter(this);
        gridview.setAdapter(vms);

        gridview.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    showVMDetails(position);
                }
            });
    }

    private void ensureInitialVMSetup()
    {
        copyVMFromApk("duke1", "Duke Nukum 1");
        copyVMFromApk("keen1", "Commander Keen 1");
        copyVMFromApk("prince1", "Prince of Persia 1");
    }
    
    private void copyVMFromApk(String vm, String name)
    {
        String floppy = ensureFileCopiedToSDFromApk(vm, "floppy.img", "floppy.img");
        String hd = ensureFileCopiedToSDFromApk(vm, vm+".img", vm+".img");
        ensureFileCopiedToSDFromApk(vm, vm+".ss", "snapshot.ss");
        ensureFileCopiedToSDFromApk(vm, vm+"vga.ss", "vga.ss");
        ensureFileCopiedToSDFromApk(vm, vm+".jpg", "screenshot.jpg");
        writeTextToFile(vm, "config.txt", "-boot fda -hda "+hd + " -fda "+floppy);
        writeTextToFile(vm, "title.txt", name);
        System.out.println("Copied VM to SD card");
    }

    public void showVMDetails(int position)
    {
        Intent i = new Intent(this, DetailsActivity.class);
        i.putExtra("vm", (String)vms.getItem(position));
        startActivityForResult(i, 100);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // regenerate thumb index
        vms.regenerateIndex(this);
        gridview.requestLayout();
        gridview.setAdapter(vms);
    }

    private String writeTextToFile(String vm, String name, String text)
    {
        File dir = new File(getExternalFilesDir(null), vm);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, name);
        try
        {
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            w.write(text);
            w.flush();
            w.close();
            return file.getAbsolutePath();
        } catch (IOException e)
        {e.printStackTrace();}
        return null;
    }

    public static String writeDataToFile(File base, String vm, String name, byte[] data)
    {
        File dir = new File(base, vm);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, name);
        try
        {
            OutputStream out = new FileOutputStream(file);
            out.write(data, 0, data.length);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (IOException e)
        {e.printStackTrace();}
        return null;
    }

    public static byte[] getFileData(File base, String name)
    {
        File file = new File(base, name);
        if (!file.exists())
            return null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try {
            InputStream in = new FileInputStream(file);
            int read;
            while ((read = in.read(buf)) >= 0)
                bout.write(buf, 0, read);
            return bout.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTextOfFile(File base, String vm, String name)
    {
        File dir = new File(base, vm);
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, name);
        try
        {
            String line;
            BufferedReader r = new BufferedReader(new FileReader(file));
            StringBuilder b = new StringBuilder();
            while ((line = r.readLine()) != null)
                b.append(line + "\n");
            r.close();
            return b.toString().trim();
        } catch (IOException e)
        {e.printStackTrace();}
        return null;
    }

    private String ensureFileCopiedToSDFromApk(String vm, String from, String to)
    {
        //check if files exists
        File dir = new File(getExternalFilesDir(null), vm);
        if (!dir.exists())
            dir.mkdirs();
        File target = new File(dir, to);
        if (target.exists())
            return target.getAbsolutePath();

        //check access
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
            return null;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
            return null;
        }
        //copy file
        try
        {
            InputStream in = this.getAssets().open(from);
            byte[] buf = new byte[4096];
            OutputStream out = new FileOutputStream(target);
            int b = 0;
            while ((b = in.read(buf)) > 0)
            {
                out.write(buf, 0, b);
            }
            in.close();
            out.flush();
            out.close();
            return target.getAbsolutePath();
        } catch (IOException e)
        {e.printStackTrace();}
        return null;
    }
}
