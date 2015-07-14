package org.jpc;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.os.*;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.inputmethodservice.*;
import java.util.logging.*;
import android.widget.*;
import android.graphics.*;
import java.io.*;
import org.jpc.emulator.*;
import org.jpc.emulator.pci.peripheral.*;
import org.jpc.j2se.*;
import org.jpc.R;

public class JPC extends Activity
{
    public static final Logger LOGGING = Logger.getLogger(JPC.class.getName());
    public static Context context = null;
    public static final String START_TOAST = "For keyboard, help or to reset the game use the menu...";
    private static final int MENU_KEYBOARD = 1;
    private static final int MENU_NEWPC = 2;
    private static final int MENU_RESETPC = 3;
    private static final int MENU_HELP = 4;
    private static final int MENU_ABOUT = 5;
    private static final int MENU_EXIT = 6;
    private String floppy, hd, vm;
    private String[] args;
    private PC pc;
    private JPCView view;
    public static boolean IS_OUYA = ConSoul.IS_OUYA;

    public void onCreate(Bundle savedInstanceState)
    {
        System.out.println("OnCreate JPC");
        super.onCreate(savedInstanceState);
        // start progress bar
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();

        Intent intent = getIntent();
        args = intent.getStringExtra("args").split(" ");
        System.out.println("args = " + intent.getStringExtra("args"));
        vm = intent.getStringExtra("vm");
        context = this;
        if (IS_OUYA)
            setContentView(R.layout.main_ouya);
        else
            setContentView(R.layout.main_tablet);

        long heap = Runtime.getRuntime().maxMemory()/1048576;
        if (heap < 24)
            return;

        new CreatePCTask(dialog, this).execute(savedInstanceState);
    }

    private class CreatePCTask extends AsyncTask<Bundle, Void, Void> {
        ProgressDialog dialog;
        JPC jpc;
        KeyboardView keyboardView;

        private CreatePCTask(ProgressDialog d, JPC jpc)
        {
            super();
            dialog = d;
            this.jpc = jpc;
        }
        
        protected Void doInBackground(Bundle... bundles)
        {
            try {
                Bundle savedInstanceState = bundles[0];
                byte[] ss = null;
                byte[] vga = null;
                try {
                    ss = savedInstanceState.getByteArray("snapshot");
                } catch (NullPointerException e) {}
                pc = new PC(new VirtualClock(), args);
                if (ss == null)
                {
                    // try loading snapshot from disk
                    ss = DetailsActivity.getSnapshot(getExternalFilesDir(null), vm);
                    if (savedInstanceState != null)
                        vga = savedInstanceState.getByteArray("vga");
                    else
                        vga = DetailsActivity.getVGA(getExternalFilesDir(null), vm);
                }
                if (ss != null)
                {
                    try {
                        pc.loadState(new ByteArrayInputStream(ss));
                    } catch (IOException e) {e.printStackTrace();}
                }
                view = (JPCView) findViewById(R.id.JPCView);
                view.setPC(pc);
                view.setActivity(JPC.this);
                keyboardView = (KeyboardView) findViewById(R.id.keyboardView);
                //view.initialiseGUI(JPC.this, keyboardView);
                ((DefaultVGACard) pc.getComponent(VGACard.class)).setView(view);
            
                if (ss != null)
                    ((VGACard) pc.getComponent(VGACard.class)).setOriginalDisplaySize();
                else 
                    ((DefaultVGACard) pc.getComponent(VGACard.class)).resizeDisplay(720, 480);
                if ((savedInstanceState == null) && (vga != null))
                {
                    savedInstanceState = new Bundle();
                    savedInstanceState.putByteArray("vga", vga);
                }
                view.loadState(savedInstanceState);

            } catch (IOException e)
            {
                Toast toast = Toast.makeText(jpc, "I/O Error creating PC", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                e.printStackTrace();
                System.exit(0);
            }
            return null;
        }
        
        protected void onProgressUpdate() {
        }
        
        protected void onPostExecute(Void res) {
            System.out.println("Created PC!");
            view.initialiseGUI(JPC.this, keyboardView);
            view.requestLayout();
            // fix opacity and start with keyboard not visible
            System.out.println("1 keyboard visible = " + view.isKeyboardVisible());
            view.toggleKeyboardVisibility();
            System.out.println("2 keyboard visible = " + view.isKeyboardVisible());
        
            // if portrait start with keyboard visible
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
                view.toggleKeyboardVisibility();

            // show toast pointing to menu
            Toast toast = Toast.makeText(jpc, START_TOAST, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            dialog.dismiss();
            onResume();
        }
    }

    protected void onStart()
    {
        super.onStart();
        long heap = Runtime.getRuntime().maxMemory()/1048576;
        System.out.println("OnStart: Heap size: " + heap);
        if (heap < 32)
        {
            AlertDialog a = new AlertDialog.Builder(this).create();
            a.setTitle("Insufficient memory");
            a.setMessage("Not enough memory to run: " + heap + "mb (need at least 32mb).");
            a.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    } });
            a.show();
        }
        System.out.println("Started JPC");
    }

    protected void onResume() {
        System.out.println("resuming JPC...");
        super.onResume();
        if (view != null)
            view.startThreads();
        System.out.println("resumed JPC.");
    }

    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    private void saveState(Bundle outState)
    {
        System.out.println("Saving JPC state...");
        view.stopThreads();
        //snapshot to byte[]
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            pc.saveState(bout);
        } catch (IOException e) {e.printStackTrace();}
        outState.putByteArray("snapshot", bout.toByteArray());
        outState.putInt("width", ((VGACard) pc.getComponent(VGACard.class)).getDisplaySize()[0]);
        outState.putInt("height", ((VGACard) pc.getComponent(VGACard.class)).getDisplaySize()[1]);
        outState.putBoolean("started", true);
        outState.putString("vm", vm);
        StringBuilder b = new StringBuilder();
        for (String s: args)
            b.append(s + " ");
        outState.putString("args", b.toString().trim());
        //save screen contents
        view.saveState(outState);
        System.out.println("Saved JPC state.");
    }

    protected void onPause() {
        System.out.println("pausing JPC...");
        super.onPause();
        view.stopThreads();
        System.out.println("paused JPC.");
    }

    protected void onStop() {
        System.out.println("Stopping JPC...");
        super.onStop();
        if (view != null)
            view.stopThreads();
        System.out.println("Stopped JPC.");
    }

    protected void onDestroy() {
        System.out.println("destroy JPC...");
        super.onDestroy();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_KEYBOARD, 0, R.string.menu_keyboard);
        //menu.add(0, MENU_NEWPC, 0, R.string.menu_newpc);
        menu.add(0, MENU_RESETPC, 0, R.string.menu_resetpc);
        menu.add(0, MENU_HELP, 0, R.string.menu_help);
        menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
        menu.add(0, MENU_EXIT, 0, R.string.menu_exit);
        return true;
    }

    public void showHelp()
    {
        System.out.println("Showing help");
        Intent i = new Intent(this, TextActivity.class);
        if (IS_OUYA)
            i.putExtra("text", "help_ouya.html");
        else
            i.putExtra("text", "help_tablet.html");
        this.startActivity(i);
    }

    public void showAbout()
    {
        System.out.println("Showing About");
        Intent i = new Intent(this, TextActivity.class);
        if (IS_OUYA)
            i.putExtra("text", "about_ouya.html");
        else
            i.putExtra("text", "about_tablet.html");
        this.startActivity(i);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_KEYBOARD:
            view.toggleKeyboardVisibility();
            break;
        case MENU_NEWPC:
            break;
        case MENU_RESETPC:
            view.stopThreads();
            try 
            {
                pc = new PC(new VirtualClock(), args);
            } catch (IOException e)
            {
                e.printStackTrace(); 
                break;
            }
            ((DefaultVGACard) pc.getComponent(VGACard.class)).setView(view);
            view.setPC(pc);
            view.startThreads();
            break;
        case MENU_HELP:
            showHelp();
            break;
        case MENU_ABOUT:
            showAbout();
            break;
        case MENU_EXIT:
            System.out.println("Exiting emulator...");
            Bundle b = new Bundle();
            saveState(b);
            DetailsActivity.saveSnapshot(getExternalFilesDir(null), vm, b.getByteArray("snapshot"), ((VGACard) pc.getComponent(VGACard.class)).getDisplayBuffer(), b.getInt("width"), b.getInt("height"));
            DetailsActivity.saveVGA(getExternalFilesDir(null), vm, b.getByteArray("vga"));
            //Intent intent = new Intent();
            //intent.putExtras(b);
            setResult(Activity.RESULT_OK, new Intent());
            System.out.println("Emulator finishing...");
            finish();
            break;
        }
        return true;
    }
}
