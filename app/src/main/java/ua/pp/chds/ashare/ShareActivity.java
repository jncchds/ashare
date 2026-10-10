package ua.pp.chds.ashare;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Invisible activity that turns "open this file" into "share this file".
 *
 * It stays alive (transparent) behind the share sheet instead of finishing right away: the
 * read grant we received for the URI belongs to this activity, and the share sheet launches
 * the chosen app on our behalf, so the grant must still be valid at that point.
 */
public class ShareActivity extends Activity {

    private static final int REQUEST_PICK = 1;
    private static final int REQUEST_SHARE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // Recreated (e.g. rotation) while the picker or share sheet is already showing.
            return;
        }

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            share(intent.getData(), intent.getType());
        } else {
            pick();
        }
    }

    private void pick() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*");
        try {
            startActivityForResult(pick, REQUEST_PICK);
        } catch (ActivityNotFoundException e) {
            fail();
        }
    }

    private void share(Uri uri, String type) {
        if (type == null) {
            type = getContentResolver().getType(uri);
        }
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType(type != null ? type : "*/*")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // ClipData is what actually carries the URI grant, and lets the share sheet preview the file.
        send.setClipData(ClipData.newRawUri(null, uri));
        try {
            startActivityForResult(Intent.createChooser(send, null), REQUEST_SHARE);
        } catch (ActivityNotFoundException | SecurityException e) {
            fail();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            share(data.getData(), null);
        } else {
            finish();
        }
    }

    private void fail() {
        Toast.makeText(this, R.string.share_failed, Toast.LENGTH_LONG).show();
        finish();
    }
}
