package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import de.dagere.peass.execution.gradle.ManifestEditor;

public class TestManifestEditor {
    @Test
    public void testManifestUpdateForReadWrite() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        final String manifestConent = String.join("\n",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"",
        "   package=\"com.example.android_example\">",
        "   <application",
        "      android:allowBackup=\"true\"",
        "      android:icon=\"@mipmap/ic_launcher\"",
        "      android:label=\"@string/app_name\"",
        "      android:roundIcon=\"@mipmap/ic_launcher_round\"",
        "      android:supportsRtl=\"true\"",
        "      android:theme=\"@style/Theme.Androidexample\">",
        "      <activity",
        "         android:name=\".MainActivity\"",
        "         android:label=\"@string/app_name\"",
        "         android:theme=\"@style/Theme.Androidexample.NoActionBar\">",
        "         <intent-filter>",
        "            <action android:name=\"android.intent.action.MAIN\" />",
        "            <category android:name=\"android.intent.category.LAUNCHER\" />",
        "         </intent-filter>",
        "      </activity>",
        "   </application>",
        "</manifest>"
        );
      
        TemporaryFolder tempFolder = new TemporaryFolder();
        tempFolder.create();
        
        File manifestFile = tempFolder.newFile("AndroidManifest.xml");
        
        FileWriter fw = new FileWriter(manifestFile);
        fw.write(manifestConent);
        fw.close();

        ManifestEditor editor = new ManifestEditor(manifestFile);
        editor.updateForExternalStorageReadWrite();

        final String manifestFileContentsAfterUpdate = FileUtils.readFileToString(manifestFile, Charset.defaultCharset());

        MatcherAssert.assertThat(manifestFileContentsAfterUpdate, Matchers.containsString("<uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\""));
        MatcherAssert.assertThat(manifestFileContentsAfterUpdate, Matchers.containsString("<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\""));
        MatcherAssert.assertThat(manifestFileContentsAfterUpdate, Matchers.containsString("android:requestLegacyExternalStorage=\"true\""));
    }
}
