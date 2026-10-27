package ua.pp.chds.ashare;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Invisible activity that copies an opened file into the public Downloads folder.
 *
 * It stays alive until the copy is done: the read grant for the source URI belongs to this
 * activity. Android 10+ writes through MediaStore and needs no permission; Android 8–9 writes
 * the file directly and asks for storage permission first.
 */
public class SaveActivity extends Activity {

    private static final int REQUEST_STORAGE = 1;

    private Uri source;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        source = getIntent().getData();
        type = getIntent().getType();
        if (source == null) {
            fail();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
        } else {
            save();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            save();
        } else {
            fail();
        }
    }

    private void save() {
        Context app = getApplicationContext();
        Toast.makeText(app, R.string.saving, Toast.LENGTH_SHORT).show();
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            String saved;
            try {
                saved = copy(app.getContentResolver());
            } catch (IOException | RuntimeException e) {
                saved = null;
            }
            String name = saved;
            main.post(() -> {
                if (name != null) {
                    Toast.makeText(app, app.getString(R.string.saved, name), Toast.LENGTH_LONG)
                            .show();
                    finish();
                } else {
                    fail();
                }
            });
        }).start();
    }

    /** Copies the source into Downloads and returns the file name it was saved under. */
    private String copy(ContentResolver resolver) throws IOException {
        if (type == null) {
            type = resolver.getType(source);
        }
        String name = displayName(resolver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            if (type != null) {
                values.put(MediaStore.Downloads.MIME_TYPE, type);
            }
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (target == null) {
                throw new IOException("MediaStore insert failed");
            }
            try (InputStream in = resolver.openInputStream(source);
                 OutputStream out = resolver.openOutputStream(target)) {
                pipe(in, out);
            } catch (IOException | RuntimeException e) {
                resolver.delete(target, null, null);
                throw e;
            }
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(target, values, null, null);
            // MediaStore may have renamed it to avoid a clash, e.g. "file (1).pdf".
            String finalName = queryName(resolver, target);
            return finalName != null ? finalName : name;
        }

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("No Downloads folder");
        }
        File file = uniqueFile(dir, name);
        try (InputStream in = resolver.openInputStream(source);
             OutputStream out = new FileOutputStream(file)) {
            pipe(in, out);
        } catch (IOException | RuntimeException e) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            throw e;
        }
        return file.getName();
    }

    private String displayName(ContentResolver resolver) {
        String name = queryName(resolver, source);
        if (name == null) {
            name = source.getLastPathSegment();
        }
        if (name != null) {
            // Some providers return a path; keep only the last part and drop unsafe characters.
            name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\\\:*?\"<>|]", "_");
        }
        if (name == null || name.isEmpty() || name.startsWith(".")) {
            name = "file" + (name != null ? name : "");
        }
        if (name.indexOf('.') < 0 && type != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
            if (ext != null) {
                name += "." + ext;
            }
        }
        return name;
    }

    private static String queryName(ContentResolver resolver, Uri uri) {
        try (Cursor c = resolver.query(
                uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            }
        } catch (RuntimeException ignored) {
            // Not every provider supports queries.
        }
        return null;
    }

    private static File uniqueFile(File dir, String name) {
        File file = new File(dir, name);
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; file.exists(); i++) {
            file = new File(dir, base + " (" + i + ")" + ext);
        }
        return file;
    }

    private static void pipe(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) {
            throw new IOException("Can't open stream");
        }
        byte[] buffer = new byte[64 * 1024];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
    }

    private void fail() {
        Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
        finish();
    }
}
