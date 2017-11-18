package cookiework.encryptedvideoview;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cookiework.encryptedvideoview.encryption.VideoInfo;

public class VideoDetailActivity extends AppCompatActivity {
    private VideoInfo info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        info = intent.getParcelableExtra("videoInfo");

        TextView lblTitle = (TextView) findViewById(R.id.lblTitle_Detail);
        TextView lblIntro = (TextView) findViewById(R.id.lblIntro_Detail);

        lblTitle.setText("标题：" + info.getCipherTitle());
        lblIntro.setText("简介：" + info.getCipherIntro());
        System.out.println(info.getCipherAddr());

        Button btnOpenVideo = (Button) findViewById(R.id.btnOpenVideo);
        btnOpenVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent playIntent = new Intent(VideoDetailActivity.this, VideoPlayActivity.class);
                playIntent.putExtra("videoInfo", info);
                startActivity(playIntent);
            }
        });

        if(info.getStatus().equals("transform")){
            btnOpenVideo.setEnabled(false);
            btnOpenVideo.setText("转码中，请稍后观看。");
        } else if(info.getStatus().equals("end")){
            btnOpenVideo.setEnabled(false);
            btnOpenVideo.setText("直播已结束。");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
