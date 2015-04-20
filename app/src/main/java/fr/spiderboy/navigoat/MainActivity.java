package fr.spiderboy.navigoat;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    private NfcAdapter mNfcAdapter;
    public static final String dTag = "Navigoat";
    private Navigo card;
    private CustomListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        ListView lView = (ListView) findViewById(R.id.listView);
        listAdapter = new CustomListAdapter(this);
        lView.setAdapter(listAdapter);
        listAdapter.add("Scan your card !");

        /// Listener for verbose checkbox
        SharedPreferences.OnSharedPreferenceChangeListener changeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("verbose_checkbox")) {
                    ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.profileSwitcher);
                    switcher.showNext();
                }
            }
        };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!preferences.getBoolean("verbose_checkbox", true)) {
            ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.profileSwitcher);
            switcher.showNext();
        }
        preferences.registerOnSharedPreferenceChangeListener(changeListener);

        TextView mTextView = (TextView) findViewById(R.id.text_view_main);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        if (mNfcAdapter == null) {
            mTextView.setText("NFC not supported on this device. Go get a new one.\n");
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText("NFC is not enabled. Go enable it.\n");
        } else {
            mTextView.setText("Waiting for card...\n");
        }

        handleIntent(getIntent());
    }

    private void addText(String text) {
        TextView mTextView = (TextView) findViewById(R.id.text_view_main);
        if (mTextView != null) {
            mTextView.append(text + "\n");
        }
    }

    private void addElement(String text) {
        listAdapter.add(text);
    }



    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String sIsoDep = IsoDep.class.getName();
            String sNfcB = NfcB.class.getName();
            for (String tech : tag.getTechList()) {
                if (tech.equals(sIsoDep) || tech.equals(sNfcB)) {
                    TextView mTextView = (TextView) findViewById(R.id.text_view_main);
                    mTextView.setText("Waiting for card...\n");
                    listAdapter.clear();
                    card = new Navigo(tag.getId(), getResources().getXml(R.xml.card_struct),
                                        getResources().getXml(R.xml.stations));
                    addText("Found tag class " + tech);
                    synchronized (this) {
                        new NfcReaderTask().execute(tag);
                    }
                    break;
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{
                new String[] {
                        IsoDep.class.getName(),
                        NfcB.class.getName(),
                }
        };

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link MainActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private class NfcReaderTask extends AsyncTask<Tag, Void, String> {
        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];
            IsoDep iso = IsoDep.get(tag);
            try {
                return readTag(iso);
            } catch (IOException e) {
                return "Could not read tag: " + e.getMessage();
            }
        }

        private String readTag(IsoDep iso) throws IOException {
            final IsoDep isodep = iso;
            isodep.connect();
            isodep.setTimeout(5000);
            if (isodep.isConnected()) {
                try {
                    synchronized (this) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addText("Connected!");
                                addText("Parsing card...");
                                card.parseIsoDep(isodep);
                                addText("Dumping card...");
                                card.dump();
                                for (String elt : card.getElements())
                                    addElement(elt);
                            }
                        });
                        wait(1000);
                        return card.getDump();
                    }
                } catch (InterruptedException e) {
                    return "Thread interrupted: " + e.getMessage();
                }
            } else {
                return "Could not connect to card " + card.getId();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                addText(result);
            }
        }
    }
}
