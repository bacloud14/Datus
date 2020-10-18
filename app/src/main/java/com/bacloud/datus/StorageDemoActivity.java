package com.bacloud.datus;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
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
import androidx.core.text.HtmlCompat;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class StorageDemoActivity extends AppCompatActivity {

    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;
    private EditText textEditMeta;
    private EditText textEditMetaDeep;
    private TextView textViewASCII;
    private EditText textEditHex;

    private long size = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_storage_demo);
        textEditMeta = findViewById(R.id.textEditMeta);
        textEditMetaDeep = findViewById(R.id.textEditMetaDeep);
        textViewASCII = findViewById(R.id.textViewASCII);
        textEditHex = findViewById(R.id.textEditHex);

    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);
        Uri currentUri;
        String content = "";
        String output = "";
        String alias;
        MediaTypeRegistry registry = MediaTypeRegistry.getDefaultRegistry();
        if (resultCode != Activity.RESULT_OK || requestCode != OPEN_REQUEST_CODE || resultData == null) {
            Toast.makeText(getBaseContext(), "success",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean error;

        currentUri = resultData.getData();
        try {
            String language = getString(R.string.not_plain_text);
            String type;
            String extension = "";
            String size;
            MediaType mimeType = null;
            // FILE EXTENSION DETECTION :: Tika client code for
            // FILE EXTENSION DETECTION :: Tika client code for
            // FILE EXTENSION DETECTION :: Tika client code for
            try {
                mimeType = detectMediaType(currentUri);
                type = mimeType.getType();
                content = readFileContent(currentUri, mimeType.getType().equals("text"));
                if (mimeType.getType().equals("text") && mimeType.getSubtype().equals("plain"))
                    language = detectLang(content);
                size = getMediaSize(currentUri);
                String[] native_tags = dumpImageMetaData(this, currentUri);
                Set<MediaType> aliases = registry.getAliases(mimeType);
                alias = aliases.isEmpty() ? "" : String.format("<br><font color='#008577'>%s is known as %s</font>", mimeType, aliases);
                extension = detectExtension(mimeType);
                output = s(String.format(
                        "Name: %s " +
                                "<br><font color='#1ABC9C'>Type: %s</font>%s" +
                                "<br>Language: %s" +
                                "<br>Extension: %s" +
                                "<br>Size: %s"
                        , native_tags[0], type, alias, language, extension, size));
                error = false;
            } catch (TikaException | IllegalAccessException | InstantiationException | RuntimeException e) {
                error = true;
                Toast.makeText(getBaseContext(), "failed",
                        Toast.LENGTH_LONG).show();
                output = s(getString(R.string.error_reading_file));
                e.printStackTrace();
            }

            HTML(textEditMeta, output);
            // METADATA DETECTION
            // METADATA DETECTION
            // METADATA DETECTION
            // PDF and IMAGE metadata detection
            // PdfBox-Android and com.drewnoakes metadata-extractor
            ArrayList<String> mediaMetadata = new ArrayList<>();
            ArrayList<String> pdfMetadata = new ArrayList<>();
            ArrayList<String> nativeMetadata = new ArrayList<>();
            try {
                mediaMetadata = getImageAndVideosMetadata(currentUri);
            } catch (RuntimeException e) {
                error = true;
                e.printStackTrace();
            }
            try {
                if (extension.equals(".pdf"))
                    pdfMetadata = getPdfMetadata(currentUri);
            } catch (RuntimeException e) {
                error = true;
                e.printStackTrace();
            }


            mediaMetadata.addAll(pdfMetadata);
            if (!mediaMetadata.isEmpty() || !pdfMetadata.isEmpty()) {
                String text = stringBuilderFormatted(mediaMetadata);
                HTML(textEditMetaDeep, text);
            } else {
                // NATIVE MEDIA METADATA DETECTION
                // NATIVE MEDIA METADATA DETECTION
                // NATIVE MEDIA METADATA DETECTION

                if (mimeType != null && (mimeType.getType().equals("audio") ||
                        mimeType.getType().equals("image") ||
                        mimeType.getType().equals("video")))
                    nativeMetadata = nativeGetMetadata(currentUri);

                if (!nativeMetadata.isEmpty()) {
                    output = stringBuilderFormatted(nativeMetadata);
                    HTML(textEditMetaDeep, output);
                } else {
                    // RAW CONTENT OUTPUT
                    // RAW CONTENT OUTPUT
                    // RAW CONTENT OUTPUT
                    textViewASCII.setText(R.string.ascii_preview);
                    if (content.length() >= 200)
                        HTML(textEditMetaDeep, s(content.substring(0, 200)));
                    else
                        HTML(textEditMetaDeep, s(content));
                }
            }
        } catch (IOException | IllegalAccessException e) {
            // Handle error here
            error = true;
            e.printStackTrace();
        }
        if (!error)
            Toast.makeText(getBaseContext(), "success",
                    Toast.LENGTH_LONG).show();

    }

    private void HTML(EditText textView, String text) {
        textView.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
    }

    private String stringBuilderFormatted(ArrayList<String> metadata) {
        StringBuilder builder = new StringBuilder();
        for (String value : metadata) {
            builder.append(value);
        }
        return s(builder.toString());
    }

    private ArrayList<String> nativeGetMetadata(Uri currentUri) throws IOException, IllegalAccessException, NullPointerException {
        ArrayList<String> directories = new ArrayList<>();
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        ParcelFileDescriptor pfd = this
                .getContentResolver()
                .openFileDescriptor(currentUri, "r");

        if (pfd != null) {
            metaRetriever.setDataSource(pfd.getFileDescriptor());
            for (Field f : MediaMetadataRetriever.class.getFields()) {
                f.setAccessible(true);
                Object value = f.get(metaRetriever);
                Object o = metaRetriever.extractMetadata((Integer) value);
                if (o != null) {
                    directories.add(String.format("<font color='#3498DB'>%s= </font> %s<br>",
                            f.getName(), o));
                }
            }
            metaRetriever.close();
            pfd.close();
        }
        return directories;
    }

    public String[] dumpImageMetaData(Context context, Uri uri) {
        String displayName = "";
        String size = "";
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.

        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
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
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                } else {
                    size = "Unknown";
                }
            }
        }
        return new String[]{displayName, size};
    }


    private ArrayList<String> getPdfMetadata(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        PDFBoxResourceLoader.init(getApplicationContext());
        PDDocumentInformation info = new PDDocumentInformation();
        if (inputStream != null) {
            PDDocument pdf = PDDocument.load(inputStream);
            info = pdf.getDocumentInformation();
            inputStream.close();
        }
        return PDDocumentInformationFormat.format(info);
    }

    private ArrayList<String> getImageAndVideosMetadata(Uri uri) throws IOException, RuntimeException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ArrayList<String> directories = new ArrayList<>();
        try {
            com.drew.metadata.Metadata metadata2 = ImageMetadataReader.readMetadata(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            for (Directory directory : metadata2.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    directories.add(String.format("<font color='#3498DB'>%s - %s= </font> %s<br>",
                            directory.getName(), tag.getTagName(), tag.getDescription()));
                }
                if (directory.hasErrors()) {
                    for (String error : directory.getErrors()) {
                        directories.add("<font color='#C0392B'>ERROR: " + error + "</font><br>");
                    }
                }
            }
        } catch (ImageProcessingException e) {
            return new ArrayList<>();
        }
        return directories;

    }

    // READ LIMIT CONTENT PARSING TO SOME EXTENT NOT TO BE SO HEAVY
    private String readFileContent(Uri uri, boolean textual) throws IOException, InstantiationException, IllegalAccessException {

        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream != null) {
            if (textual) {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(
                                inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String currentLine;
                int co = 0;
                while (co < 10 && (currentLine = reader.readLine()) != null) {
                    stringBuilder.append(currentLine).append("\n");
                    co++;
                }
                inputStream.close();
                return stringBuilder.toString();
            } else {
                byte[] fileContent = new byte[200];
                inputStream.read(fileContent, 0, 200);
                String s = new String(fileContent);
                String hexCode = s(Utils.hex(fileContent));
                textEditHex.setText(Html.fromHtml(hexCode));
                inputStream.close();
                return s;
            }
        }
        return "";
    }

    public void openFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public MediaType detectMediaType(Uri uri) throws TikaException, IOException, RuntimeException {
        InputStream inputStream =
                getContentResolver().openInputStream(uri);
        TikaConfig tika = new TikaConfig();

        MediaType mimetype = null;
        if (inputStream != null) {
            mimetype = tika.getDetector().detect(
                    TikaInputStream.get(inputStream), new Metadata());
            inputStream.close();
        }

        return mimetype;
    }

    public String detectLang(String content) {
        LanguageIdentifier identifier = new LanguageIdentifier(content);
        return identifier.getLanguage();
    }

    public String detectExtension(MediaType mediatype) {
        TikaConfig tika;
        String extension = "";
        AtomicReference<String> mimeTypeRef = new AtomicReference<>(null);
        mimeTypeRef.set(mediatype.toString());

        String mimeType = mimeTypeRef.get();
        try {
            MimeType mimetype;
            tika = new TikaConfig();
            mimetype = tika.getMimeRepository().forName(mimeType);
            extension = mimetype.getExtension();

            if (mimeType != null && mimeType.equals("application/gzip") && extension.equals(".tgz")) {
                extension = ".gz";
            }

        } catch (TikaException | IOException e) {
            e.printStackTrace();
        }

        return extension;

    }

    public String getMediaSize(Uri uri) throws FileNotFoundException {
        InputStream inputStream =
                getContentResolver().openInputStream(uri);

        try (final TikaInputStream tis = TikaInputStream.get(inputStream)) {
            size = tis.getLength();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Utils.readableFileSize(size);
    }

    public void thanks(View view) {
        String to = view.getTag().toString();
        String message = "";
        try {
            AssetManager am = getApplicationContext().getAssets();
            InputStream is = am.open(to);
            InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
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
        Dialog customDialog = new Dialog(StorageDemoActivity.this);
        customDialog.setContentView(R.layout.licence_layout);

        Window window = customDialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }

        TextView tv = customDialog.findViewById(R.id.tv);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tv.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
        tv.setText(message);
        customDialog.show();
    }

    public void showLicences(View view) {
        startActivity(new Intent(this, OssLicensesMenuActivity.class));
        OssLicensesMenuActivity.setActivityTitle(getString(R.string.thanks));

        Toast.makeText(getBaseContext(), R.string.special_thanks,
                Toast.LENGTH_LONG).show();
    }

    // TAG SMALL FOR HTML OUTPUT
    public String s(String s) {
        return "<small>" + s + "</small>";
    }

}


