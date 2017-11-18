package cookiework.encryptedvideoview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import cookiework.encryptedvideoview.encryption.PtWittEnc;
import cookiework.encryptedvideoview.encryption.SubscriptionInfo;
import cookiework.encryptedvideoview.encryption.VideoInfo;
import cookiework.encryptedvideoview.util.HttpUtil;
import cookiework.encryptedvideoview.util.JsonUtil;

import static cookiework.encryptedvideoview.Constants.SERVER_ADDRESS;
import static java.net.HttpURLConnection.HTTP_OK;

public class VideoListActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private VideoListTask mAuthTask;
    private ListViewCompat listView;
    private SubscriptionInfo info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list);

        Intent intent = getIntent();
        info = intent.getParcelableExtra("info");
        setTitle(info.toString());

        listView = (ListViewCompat) this.findViewById(R.id.main_list);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("处理中，请稍等……");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);

        if (mAuthTask == null) {
            showProgress(true);
            mAuthTask = new VideoListTask(this);
            mAuthTask.execute((Void) null);
        }
    }

    public class VideoListTask extends AsyncTask<Void, Void, Boolean> {
        private HttpUtil util = new HttpUtil();
        private ArrayList<VideoInfo> infos;
        private Context context;
        private PtWittEnc enc;

        public VideoListTask(Context context) {
            this.context = context;
            this.enc = new PtWittEnc(context);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                if(infos.size() == 0){
                    listView.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1, new String[]{"该用户在此标签下尚未发布视频。"}));
                } else {
                    listView.setAdapter(new ArrayAdapter<VideoInfo>(context, android.R.layout.simple_expandable_list_item_1, infos));
                    listView.setOnItemClickListener(new VideoListItemClickListener(context));
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
                HashMap<String, String> params = new HashMap<>();
                params.put("username", info.getDestUserID());
                params.put("tStar", info.getTagName());
                util.setMethod(HttpUtil.HttpRequestMethod.POST)
                        .setUrl(SERVER_ADDRESS + "/viewer/messages")
                        .setQuery(params)
                        .sendHttpRequest();
                if (util.getResponseCode() != HTTP_OK) {
                    System.out.println(util.getResponseMessage());
                    return false;
                } else {
                    InputStream resultStream = util.getInputStream();
                    String result = HttpUtil.convertInputStreamToString(resultStream);
                    infos = JsonUtil.convertJsonToArray(result, VideoInfo.class);
                    VideoInfo.decryptVideoInfo(enc, infos, info);
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

    public class VideoListItemClickListener implements AdapterView.OnItemClickListener{
        private Context context;

        public VideoListItemClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VideoInfo info = (VideoInfo) parent.getItemAtPosition(position);
            Intent intent = new Intent(context, VideoDetailActivity.class);
            intent.putExtra("videoInfo", info);
            startActivity(intent);
        }
    }
}
