package cookiework.encryptedvideoview;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;

import cookiework.encryptedvideoview.encryption.SubscriptionProcessor;
import cookiework.encryptedvideoview.util.*;

import static android.content.Context.MODE_PRIVATE;
import static cookiework.encryptedvideoview.Constants.*;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Created by Administrator on 2017/01/17.
 */

public class AddSubscribeDialog {
    private Context context;
    private View dialogView;
    private ProgressDialog progressDialog;

    public class AddSubscribeTask extends AsyncTask<Void, Void, Boolean> {
        private View dialogView;
        private String destUser;
        private String tag;
        private DialogInterface dialog;
        private boolean isNetworkFailure = false;
        private HttpUtil util = new HttpUtil();
        private SubscriptionProcessor processor = new SubscriptionProcessor(context);
        private int id;
        private String rStr;

        public AddSubscribeTask(View dialogView, DialogInterface dialog) {
            this.dialogView = dialogView;
            this.dialog = dialog;
        }

        @Override
        protected void onPreExecute() {
            EditText txtDestUser = (EditText) dialogView.findViewById(R.id.addsub_destuser);
            EditText txtDestTag = (EditText) dialogView.findViewById(R.id.addsub_tag);
            destUser = txtDestUser.getText().toString();
            tag = txtDestTag.getText().toString();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String publicKey = getPublicKey();
                if(publicKey == null){
                    return false;
                } else {
                    id = getSubscriptionId();
                    rStr = processor.getEnc().getRasString();
                    String m = processor.getRequestString(tag, rStr);
                    sendM(m);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                isNetworkFailure = true;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                DBHelper dbHelper = new DBHelper(context);
                ViewerTag viewerTag = new ViewerTag();
                viewerTag.setId(id);
                viewerTag.setTag(tag);
                viewerTag.setR(rStr);
                dbHelper.addTagItem(viewerTag);
                Toast.makeText(context, "添加关注请求成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                if (isNetworkFailure) {
                    Toast.makeText(context, R.string.info_network_error, Toast.LENGTH_LONG).show();
                } else {
                    EditText txtDestUser = (EditText) dialogView.findViewById(R.id.addsub_destuser);
                    txtDestUser.setError(context.getString(R.string.err_user_notexist));
                    txtDestUser.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private String getPublicKey() throws Exception {
            util.setMethod(HttpUtil.HttpRequestMethod.POST)
                    .setUrl(SERVER_ADDRESS + "/viewer/getpublickey")
                    .setQueryString("destUser=" + destUser)
                    .sendHttpRequest();
            if (util.getResponseCode() != HTTP_OK) {
                System.out.println(util.getResponseMessage());
                throw new Exception();
            } else {
                InputStream resultStream = util.getInputStream();
                String result = HttpUtil.convertInputStreamToString(resultStream);
                JsonObject resultObj = JsonUtil.getJsonObj(result);
                if (resultObj.get("result").getAsString().equals("success")) {
                    return "true";
                } else {
                    return null;
                }
            }
        }
        
        private int getSubscriptionId() throws Exception{
            util.setMethod(HttpUtil.HttpRequestMethod.POST)
                    .setUrl(SERVER_ADDRESS + "/viewer/createsubscribe")
                    .sendHttpRequest();
            if (util.getResponseCode() != HTTP_OK) {
                System.out.println(util.getResponseMessage());
                throw new Exception();
            } else {
                InputStream resultStream = util.getInputStream();
                String result = HttpUtil.convertInputStreamToString(resultStream);
                JsonObject resultObj = JsonUtil.getJsonObj(result);
                return resultObj.get("id").getAsInt();
            }
        }

        private void sendM(String m) throws Exception {
            SharedPreferences sp = context.getSharedPreferences(SHARED_PREFERENCES, MODE_PRIVATE);
            String username = sp.getString("username", null);
            HashMap<String, String> params = new HashMap<>();
            params.put("username", username);
            params.put("M", m);
            params.put("destUser", destUser);
            params.put("id", Integer.toString(id));
            util.setMethod(HttpUtil.HttpRequestMethod.POST)
                    .setUrl(SERVER_ADDRESS + "/viewer/addsubscribe")
                    .setQuery(params)
                    .sendHttpRequest();
            if (util.getResponseCode() != HTTP_OK) {
                System.out.println(util.getResponseMessage());
                throw new Exception();
            } else {
                InputStream resultStream = util.getInputStream();
                String result = HttpUtil.convertInputStreamToString(resultStream);
                JsonObject resultObj = JsonUtil.getJsonObj(result);
                if (resultObj.get("result").getAsString().equals("success")) {
                    return;
                } else {
                    throw new Exception();
                }
            }
        }
    }

    private AddSubscribeTask mAuthTask = null;

    private static class PreventClosingHandler extends Handler {
        private WeakReference<DialogInterface> mDialog;

        public PreventClosingHandler(DialogInterface mdialog)
        {
            mDialog = new WeakReference<>(mdialog);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case DialogInterface.BUTTON_POSITIVE:
                case DialogInterface.BUTTON_NEGATIVE:
                case DialogInterface.BUTTON_NEUTRAL:
                    ((DialogInterface.OnClickListener) msg.obj).onClick(mDialog.get(), msg.what);
                    break;
            }
        }
    }

    public AddSubscribeDialog(Context context) {
        this.context = context;
        this.dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_addsubscribe, null);
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(context.getString(R.string.info_addsub_wait));
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.mainmenu_addsubscribe);
        builder.setView(dialogView);
        builder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                processOkButton(dialog, which);
            }
        });
        builder.setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        try {
            Field field = dialog.getClass().getDeclaredField("mAlert");
            field.setAccessible(true);
            Object obj = field.get(dialog);
            field = obj.getClass().getDeclaredField("mHandler");
            field.setAccessible(true);
            field.set(obj, new PreventClosingHandler(dialog));
        } catch (Exception e) {
            e.printStackTrace();
        }
        dialog.show();
    }

    private void processOkButton(DialogInterface dialog, int which) {
        if (mAuthTask != null) {
            return;
        }
        EditText txtDestUser = (EditText) dialogView.findViewById(R.id.addsub_destuser);
        EditText txtDestTag = (EditText) dialogView.findViewById(R.id.addsub_tag);
        txtDestTag.setError(null);
        txtDestUser.setError(null);
        String destUser = txtDestUser.getText().toString();
        String tag = txtDestTag.getText().toString();
        if (destUser == null || destUser.equals("")) {
            txtDestUser.setError(context.getString(R.string.error_username_required));
            txtDestUser.requestFocus();
        } else if (tag == null || tag.equals("")) {
            txtDestTag.setError(context.getString(R.string.error_tag_required));
            txtDestTag.requestFocus();
        } else {
            showProgress(true);
            mAuthTask = new AddSubscribeTask(dialogView, dialog);
            mAuthTask.execute((Void) null);
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
