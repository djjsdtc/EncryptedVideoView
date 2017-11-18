package cookiework.encryptedvideoview;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import cookiework.encryptedvideoview.encryption.SubscriptionInfo;
import cookiework.encryptedvideoview.encryption.SubscriptionProcessor;
import cookiework.encryptedvideoview.util.HttpUtil;
import cookiework.encryptedvideoview.util.JsonUtil;

import static cookiework.encryptedvideoview.Constants.*;
import static java.net.HttpURLConnection.HTTP_OK;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private LogoutThread logoutThread;
    private ProgressDialog progressDialog;

    private void showProgress(final boolean show) {
        if(show){
            progressDialog.show();
        }else{
            progressDialog.hide();
        }
    }

    private String getUsername(){
        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return sp.getString("username", null);
    }

    private String getSessionId(){
        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        return sp.getString("sessionID", null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        TextView txtUsername = (TextView) navigationView.getHeaderView(0).findViewById(R.id.txtUsername);
        txtUsername.setText(sp.getString("username", getString(R.string.app_name)));

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.info_logout_progress));
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);

        new FinalizeThread().execute();

        Button btnAddsubscribe = (Button)findViewById(R.id.btn_addsubscribe);
        Button btnMypending = (Button)findViewById(R.id.btn_mypending);
        Button btnMyfollowing = (Button)findViewById(R.id.btn_myfollowing);
        Button btnLogout = (Button)findViewById(R.id.btn_logout);

        MainFrameListener listener = new MainFrameListener();

        btnAddsubscribe.setOnClickListener(listener);
        btnMypending.setOnClickListener(listener);
        btnMyfollowing.setOnClickListener(listener);
        btnLogout.setOnClickListener(listener);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_quit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearSessionId(){
        SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove("username");
        editor.remove("sessionID");
        editor.commit();
    }

    public class LogoutThread extends AsyncTask<Void, Void, Boolean>{
        @Override
        protected Boolean doInBackground(Void... params) {
            HashMap<String, String> logoutParam = new HashMap<>();
            logoutParam.put("username", getUsername());
            logoutParam.put("sessionID", getSessionId());
            HttpUtil util = new HttpUtil();
            try{
                util.setMethod(HttpUtil.HttpRequestMethod.POST)
                        .setUrl(SERVER_ADDRESS + "/viewer_logout")
                        .setQuery(logoutParam)
                        .sendHttpRequest();
                if(util.getResponseCode() != HTTP_OK){
                    return false;
                } else {
                    clearSessionId();
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            logoutThread = null;
            showProgress(false);

            if (success) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, R.string.info_network_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    public class MainFrameListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            int id = v.getId();

            if(id == R.id.btn_addsubscribe){
                AddSubscribeDialog dialog = new AddSubscribeDialog(MainActivity.this);
                dialog.show();
            }
            if(id == R.id.btn_mypending){
                Intent intent = new Intent(MainActivity.this, PendingActivity.class);
                startActivity(intent);
            }
            if(id == R.id.btn_myfollowing){
                Intent intent = new Intent(MainActivity.this, MyTagsActivity.class);
                startActivity(intent);
            }
            if (id == R.id.btn_logout) {
                if (logoutThread == null) {
                    showProgress(true);
                    logoutThread = new LogoutThread();
                    logoutThread.execute((Void) null);
                }
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.menu_addsubscribe){
            AddSubscribeDialog dialog = new AddSubscribeDialog(this);
            dialog.show();
        }
        if(id == R.id.menu_mypending){
            Intent intent = new Intent(this, PendingActivity.class);
            startActivity(intent);
        }
        if(id == R.id.menu_myfollowing){
            Intent intent = new Intent(this, MyTagsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.menu_logout) {
            if (logoutThread == null) {
                showProgress(true);
                logoutThread = new LogoutThread();
                logoutThread.execute((Void) null);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public class FinalizeThread extends AsyncTask<Void, Void, Void>{
        private SubscriptionProcessor processor = new SubscriptionProcessor(MainActivity.this);
        private HttpUtil util = new HttpUtil();

        @Override
        protected Void doInBackground(Void... unused) {
            HashMap<String, String> param = new HashMap<>();
            param.put("username", getUsername());
            try{
                util.setMethod(HttpUtil.HttpRequestMethod.POST)
                        .setUrl(SERVER_ADDRESS + "/viewer/myfinalize")
                        .setQuery(param)
                        .sendHttpRequest();
                if(util.getResponseCode() != HTTP_OK){
                    System.out.println(util.getResponseMessage());
                    return null;
                } else {
                    InputStream resultStream = util.getInputStream();
                    String result = HttpUtil.convertInputStreamToString(resultStream);
                    ArrayList<SubscriptionInfo> infos = JsonUtil.convertJsonToArray(result, SubscriptionInfo.class);
                    for(SubscriptionInfo info : infos){
                        if(info.getStatus().equals(SubscriptionInfo.SUBSCRIPTION_APPROVE)) {
                            doFinalize(info);
                        } else if(info.getStatus().equals(SubscriptionInfo.SUBSCRIPTION_REPLACE)) {
                            SubscriptionInfo newInfo = tagReplace(info);
                            doFinalize(newInfo);
                        }
                    }
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void doFinalize(SubscriptionInfo info) throws Exception {
            String tStar = processor.processResponseString(info.getMPrime(), processor.getEnc().getSubscriptionRand(info.getId()), info.getId());
            HashMap<String, String> param = new HashMap<>();
            param.put("id", Integer.toString(info.getId()));
            param.put("tStar", tStar);
            util.setMethod(HttpUtil.HttpRequestMethod.POST)
                    .setUrl(SERVER_ADDRESS + "/viewer/finalize")
                    .setQuery(param)
                    .sendHttpRequest();
            if(util.getResponseCode() != HTTP_OK){
                System.out.println(util.getResponseMessage());
            }
        }

        protected SubscriptionInfo tagReplace(SubscriptionInfo info) throws Exception {
            String rStr = processor.getEnc().getRasString();
            String mStr = processor.tagReplaceString(info.getId(), rStr);
            processor.getEnc().dbHelper.updateTagR(info.getId(), rStr);
            HashMap<String, String> param = new HashMap<>();
            param.put("id", Integer.toString(info.getId()));
            param.put("M", mStr);
            util.setMethod(HttpUtil.HttpRequestMethod.POST)
                    .setUrl(SERVER_ADDRESS + "/viewer/tagreplace")
                    .setQuery(param)
                    .sendHttpRequest();
            if(util.getResponseCode() != HTTP_OK){
                System.out.println(util.getResponseMessage());
                return null;
            } else {
                InputStream resultStream = util.getInputStream();
                String result = HttpUtil.convertInputStreamToString(resultStream);
                SubscriptionInfo newInfo = JsonUtil.convertJsonToObject(result, SubscriptionInfo.class);
                return newInfo;
            }
        }
    }
}
