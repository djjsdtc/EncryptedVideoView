package cookiework.encryptedvideoview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import cookiework.encryptedvideoview.encryption.SubscriptionInfo;
import cookiework.encryptedvideoview.util.DBHelper;
import cookiework.encryptedvideoview.util.HttpUtil;
import cookiework.encryptedvideoview.util.JsonUtil;

import static cookiework.encryptedvideoview.Constants.SERVER_ADDRESS;
import static cookiework.encryptedvideoview.Constants.SHARED_PREFERENCES;
import static java.net.HttpURLConnection.HTTP_OK;

public class PendingActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private PendingListTask mAuthTask;
    private ListViewCompat listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list);

        listView = (ListViewCompat) this.findViewById(R.id.main_list);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("处理中，请稍等……");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);

        if (mAuthTask == null) {
            showProgress(true);
            mAuthTask = new PendingListTask(this);
            mAuthTask.execute((Void) null);
        }
    }

    public class PendingListTask extends AsyncTask<Void, Void, Boolean> {
        private HttpUtil util = new HttpUtil();
        private ArrayList<SubscriptionInfo> infos;
        private Context context;
        private DBHelper dbHelper;

        public PendingListTask(Context context) {
            this.context = context;
            this.dbHelper = new DBHelper(context);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                if(infos.size() == 0){
                    listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1, new String[]{"没有未确认的关注请求"}));
                } else {
                    listView.setAdapter(new ArrayAdapter<SubscriptionInfo>(context, android.R.layout.simple_expandable_list_item_1, infos));
                }
            } else {
                Toast.makeText(context, R.string.info_network_error, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        @Override
        protected Boolean doInBackground(Void... unused) {
            try{
                SharedPreferences sp = getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
                String username = sp.getString("username", null);
                HashMap<String, String> params = new HashMap<>();
                params.put("username", username);
                util.setMethod(HttpUtil.HttpRequestMethod.POST)
                        .setUrl(SERVER_ADDRESS + "/viewer/mypending")
                        .setQuery(params)
                        .sendHttpRequest();
                if (util.getResponseCode() != HTTP_OK) {
                    System.out.println(util.getResponseMessage());
                    return false;
                } else {
                    InputStream resultStream = util.getInputStream();
                    String result = HttpUtil.convertInputStreamToString(resultStream);
                    infos = JsonUtil.convertJsonToArray(result, SubscriptionInfo.class);
                    for(SubscriptionInfo info : infos){
                        info.setStatus(dbHelper.getTagItem(info.getId()).getTag());
                    }
                    return true;
                }
            }
            catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
    }

    private void showProgress(final boolean show) {
        if (show) {
            progressDialog.show();
        } else {
            progressDialog.hide();
        }
    }
}
