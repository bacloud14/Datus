package com.bacloud.datus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.mime.MimeType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class StorageDemoActivity extends AppCompatActivity {

    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;
    private static EditText textView;
    private static EditText textView2;
    private static EditText textView3;

    private long size = 0;

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x ", aByte));
            // upper case
            // result.append(String.format("%02X", aByte));
        }
        return result.toString();
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_storage_demo);
        textView = (EditText) findViewById(R.id.fileText);
        textView2 = (EditText) findViewById(R.id.fileText2);
        textView3 = (EditText) findViewById(R.id.hex);

    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);
        Uri currentUri = null;
        String content = "";
        String output = "";
        String alias = "";
        MediaTypeRegistry registry = MediaTypeRegistry.getDefaultRegistry();
//        listAllTypes();
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == SAVE_REQUEST_CODE) {

                if (resultData != null) {
                    currentUri = resultData.getData();
                    writeFileContent(currentUri);
                }
            } else if (requestCode == OPEN_REQUEST_CODE) {

                if (resultData != null) {
                    currentUri = resultData.getData();

                    try {
                        String language = "not a plain text or not identified";
                        String type;
                        String extention = "";
                        String size = "";
                        try {
                            MediaType mimetype = getMediaType(currentUri);
                            if (mimetype.getType().equals("text"))
                                content = readFileContent(currentUri, true);
                            else
                                content = readFileContent(currentUri, false);

                            if (mimetype.getType().equals("text") && mimetype.getSubtype().equals("plain")) {
                                language = detectLang(content);
                            }
                            size = getMediaSize(currentUri);
//                            String native_tags = getMetaDataNative(currentUri);
                            String[] native_tags = dumpImageMetaData(this, currentUri);
//                            String path_to_file = Utils.getPath(this, currentUri);
//                            Utils.bimboum(Paths.get(path_to_file.substring(0,20)+native_tags[0]));
//                            BasicFileAttributes attr = getMetaDataNative(currentUri);
                            Set<MediaType> aliases = registry.getAliases(mimetype);
                            alias = aliases.isEmpty() ? "" : "<br><font color='#008577'>" + mimetype + " is known as " + aliases + "</font>";

                            extention = detectExtension(mimetype);
                            type = mimetype.getType();


                            output = "Name: " + native_tags[0] +
                                    "<br><font color='#1ABC9C'>Type: " + type + "</font>" +
                                    alias +
                                    "<br>Language: " + language +
                                    "<br>Extension: " + extention +
                                    "<br>Size: " + size;

                        } catch (TikaException e) {
                            e.printStackTrace();
                        }
                        textView.setText(Html.fromHtml(output));

                        ArrayList<String> metadata = getImageAndVideosMetadata(currentUri);
                        if (metadata != null) {
                            StringBuilder builder = new StringBuilder();
                            for (String value : metadata) {
                                builder.append(value);
                            }
                            String text = "<small>" + builder.toString() + "</small>";
                            textView2.setText(Html.fromHtml(text));
                        } else {
                            if (content.length() >= 200)
                                textView2.setText(Html.fromHtml("<small>" + content.substring(0, 200) + "</small>"));
                            else
                                textView2.setText(Html.fromHtml("<small>" + content + "</small>"));
                        }

                    } catch (IOException e) {
                        // Handle error here
                        e.printStackTrace();
                    }
                }
            }
        }

        Toast.makeText(getBaseContext(), "success",
                Toast.LENGTH_LONG).show();

    }

    public String[] dumpImageMetaData(Context context, Uri uri) {
        String displayName = "";
        String size = "";
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                size = null;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
            }
        } finally {
            cursor.close();
        }
        return new String[]{displayName, size};
    }

    public static String getFileNameByUri(Context context, Uri uri) {
        String fileName = "unknown";//default fileName
        Uri filePathUri = uri;
        if (uri.getScheme().toString().compareTo("content") == 0) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow("_display_name");//Instead of "MediaStore.Images.Media.DATA" can be used "_data"
                filePathUri = Uri.parse(cursor.getString(0));
                fileName = filePathUri.getLastPathSegment().toString();
            }
        } else if (uri.getScheme().compareTo("file") == 0) {
            fileName = filePathUri.getLastPathSegment().toString();
        } else {
            fileName = fileName + "_" + filePathUri.getLastPathSegment();
        }
        return fileName;
    }


    private ArrayList<String> getImageAndVideosMetadata(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ArrayList<String> directories = new ArrayList<String>();
        try {
            com.drew.metadata.Metadata metadata2 = ImageMetadataReader.readMetadata(inputStream);
            for (Directory directory : metadata2.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    directories.add(String.format("<font color='#3498DB'>[%s]</font> - %s = %s<br>",
                            directory.getName(), tag.getTagName(), tag.getDescription()));
                }
                if (directory.hasErrors()) {
                    for (String error : directory.getErrors()) {
                        directories.add("<font color='#C0392B'>ERROR: " + error + "</font><br>");
                    }
                }
            }
        } catch (ImageProcessingException e) {
            return null;
        }
        return directories;

    }

    // limit content parsing to some extent not to be so heavy
    private String readFileContent(Uri uri, boolean textual) throws IOException {

        InputStream inputStream = getContentResolver().openInputStream(uri);

        if (textual) {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                            inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String currentline;
            int co = 0;
            while (co < 10 && (currentline = reader.readLine()) != null) {
                stringBuilder.append(currentline + "\n");
                co++;
            }
            inputStream.close();
            return stringBuilder.toString();
        } else {
            byte fileContent[] = new byte[200];
            inputStream.read(fileContent, 0, 200);
            String s = new String(fileContent);
            String hexCode = "<small>" + hex(fileContent) + "</small>";
            textView3.setText(Html.fromHtml(hexCode));
            inputStream.close();
            return s;
        }


    }

    private void writeFileContent(Uri uri) {
        try {
            ParcelFileDescriptor pfd =
                    this.getContentResolver().
                            openFileDescriptor(uri, "w");

            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            String textContent =
                    textView.getText().toString();

            fileOutputStream.write(textContent.getBytes());

            fileOutputStream.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public void showLicences(View view) {
        startActivity(new Intent(this, OssLicensesMenuActivity.class));
        OssLicensesMenuActivity.setActivityTitle(getString(R.string.thanks));

        Toast.makeText(getBaseContext(), "Plus the fabulous Apache Tika https://tika.apache.org/license.html",
                Toast.LENGTH_LONG).show();

//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("text/plain");
//        startActivityForResult(intent, SAVE_REQUEST_CODE);
    }

    public MediaType getMediaType(Uri uri) throws TikaException, IOException {
        InputStream inputStream =
                getContentResolver().openInputStream(uri);
        TikaConfig tika = new TikaConfig();

        MediaType mimetype = tika.getDetector().detect(
                TikaInputStream.get(inputStream), new Metadata());
        inputStream.close();

        return mimetype;
    }

    public String detectLang(String content) {
        LanguageIdentifier identifier = new LanguageIdentifier(content);
        String language = identifier.getLanguage();
        return language;
    }

    public String detectExtension(MediaType mediatype) {
        TikaConfig tika = null;
        String extension = "";
        AtomicReference<String> mimeTypeRef = new AtomicReference<>(null);
        mimeTypeRef.set(mediatype.toString());

        String mimeType = mimeTypeRef.get();
        try {
            MimeType mimetype;
            tika = new TikaConfig();
            mimetype = tika.getMimeRepository().forName(mimeType.toString());
            extension = mimetype.getExtension();

            if (mimeType != null && mimeType.equals("application/gzip") && extension.equals(".tgz")) {
                extension = ".gz";
            }

        } catch (TikaException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return extension;

    }

    public String getMediaSize(Uri uri) throws FileNotFoundException {
        InputStream inputStream2 =
                getContentResolver().openInputStream(uri);
        try (final TikaInputStream tis = TikaInputStream.get(inputStream2)) {
            size = tis.getLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputStream2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sizeFormatted = readableFileSize(size);
        return sizeFormatted;
    }

    public void thanks(View view) {
        String to = view.getTag().toString();
        String message = "";
        try {
            AssetManager am = getApplicationContext().getAssets();
            InputStream is = am.open(to);
            InputStreamReader inputStreamReader = new InputStreamReader(is, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = "";
            StringBuilder stringBuilder = new StringBuilder();

            while ((receiveString = bufferedReader.readLine()) != null) {
                if (receiveString.equals("")) {
                    stringBuilder.append(System.getProperty("line.separator"));
                } else {
                    stringBuilder.append(receiveString);
                }
            }
            is.close();
            message = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        createDialog(message);
    }

    private void createDialog(String message) {
        Dialog custoDialog = new Dialog(StorageDemoActivity.this);
        custoDialog.setContentView(R.layout.licence_layout);

        Window window = custoDialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);


        TextView tv = custoDialog.findViewById(R.id.tv);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tv.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
        tv.setText(message);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        custoDialog.show();
    }

    private void getImageMetadata(Uri uri) throws FileNotFoundException {


        InputStream inputStream =
                getContentResolver().openInputStream(uri);

    }
}


