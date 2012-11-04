package com.izgoy.uploader;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.Toast;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

public class ShareActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.share);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        File file;

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {

                String[] projection = { MediaStore.Images.Media.DATA };
                Cursor cur = managedQuery(fileUri, projection, null, null, null);
                cur.moveToFirst();
                String path = cur.getString(cur.getColumnIndex(MediaStore.Images.Media.DATA));

                file = new File(path);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String access_key_id = prefs.getString("aws_access_key_id", "");
                String secret_access_key = prefs.getString("aws_secret_access_key", "");
                String bucket = prefs.getString("aws_bucket", "");
                String domain = prefs.getString("domain", "");

                AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(access_key_id, secret_access_key));
                String regionName = s3Client.getBucketLocation(bucket);
                s3Client.setEndpoint("s3-" + regionName + ".amazonaws.com");

                PutObjectRequest por = new PutObjectRequest(bucket, file.getName(), file);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setCacheControl("public, max-age=315360000");
                por.setMetadata(metadata);
                por.setCannedAcl(CannedAccessControlList.PublicRead);
                s3Client.putObject(por);

                String link = "http://" + domain + "/" + URLEncoder.encode(file.getName());

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("link", link);
                clipboard.setPrimaryClip(clip);

                Toast toast = Toast.makeText(getApplicationContext(), "Copied to the clipboard: " + link, Toast.LENGTH_SHORT);
                toast.show();

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, link);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);

                finish();

            }
        }
    }
}