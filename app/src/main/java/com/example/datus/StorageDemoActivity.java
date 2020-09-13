package com.example.datus;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class StorageDemoActivity extends AppCompatActivity {

    private static EditText textView;
    private static EditText textView2;
    private static EditText textView3;

    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;
    private long size = 0;
    private final Metadata metadata = new Metadata();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thanks();

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
                            Set<MediaType> aliases = registry.getAliases(mimetype);
                            alias = mimetype + ", also known as " + aliases;

                            extention = detectExtension(mimetype);
                            type = mimetype.getType();
                            output = "<font color='#008577'>Type: " + type + "</font><br>Language: " + language + "<br>Extension: " + extention + "<br>Size: " + size;
                        } catch (TikaException e) {
                            e.printStackTrace();
                        }
                        textView.setText(Html.fromHtml(output));
                        if (content.length() >= 200)
                            textView2.setText(content.substring(0, 200));
                        else
                            textView2.setText(content);
                    } catch (IOException e) {
                        // Handle error here
                    }
                }
            }
        }

        Toast.makeText(getBaseContext(), alias,
                Toast.LENGTH_LONG).show();

    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
            // upper case
            // result.append(String.format("%02X", aByte));
        }
        return result.toString();
    }

    // limit content parsing to some extent not to be so heavy
    private String readFileContent(Uri uri, boolean textual) throws IOException {

        InputStream inputStream =
                getContentResolver().openInputStream(uri);
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
            String hexCode = hex(fileContent);
            textView3.setText(hexCode);
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

    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public String getMediaSize(Uri uri) throws FileNotFoundException {
        InputStream inputStream2 =
                getContentResolver().openInputStream(uri);
        String value = metadata.get(Metadata.CONTENT_LENGTH);

        if (null != value && !value.isEmpty()) {
            size = Long.valueOf(value);
        } else {
            try (final TikaInputStream tis = TikaInputStream.get(inputStream2)) {
                size = tis.getLength();
            } catch (IOException e) {
                e.printStackTrace();
            }

            metadata.set(Metadata.CONTENT_LENGTH, Long.toString(size));
        }
        try {
            inputStream2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String sizeFormatted = readableFileSize(size);
        return sizeFormatted;
    }

    public void thanks() {
        String message = "";
        try {
            AssetManager am = getApplicationContext().getAssets();
            InputStream is = am.open("Apache_Tika_Project_License.txt");
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
            System.out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


