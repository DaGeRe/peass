package de.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.changesreading.FileComparisonUtil;
import difflib.DiffUtils;
import difflib.Patch;

public class MethodChangeReader {
   
   private final File outFolder;
   private final ChangedEntity clazz;

   private final String version;

   private final String method, methodOld;

   public MethodChangeReader(File outFolder, File sourceFolder, File oldSourceFolder, ChangedEntity clazz, String version) throws FileNotFoundException {
      this.outFolder = outFolder;
      this.clazz = clazz;
      this.version = version;
      
//      if (clazz.toString().contains("CSVFormat")) {
//         System.out.println("Test");
//      }
      
      method = FileComparisonUtil.getMethod(sourceFolder, clazz, clazz.getMethod());
      methodOld = FileComparisonUtil.getMethod(oldSourceFolder, clazz, clazz.getMethod());
   }

   public void readMethodChangeData() throws IOException {
      if (!method.equals(methodOld)) {
         File folder = new File(outFolder, version);
         folder.mkdirs();
         File slow = new File(folder, clazz.getSimpleFullName() + "_slow.txt");
         File fast = new File(folder, clazz.getSimpleFullName() + "_fast.txt");

         FileUtils.writeStringToFile(slow, method, Charset.defaultCharset());
         FileUtils.writeStringToFile(fast, methodOld, Charset.defaultCharset());
      }
   }
   
   public Patch<String> getKeywordChanges(final ChangedEntity clazz) throws FileNotFoundException {
      final Patch<String> patch = DiffUtils.diff(Arrays.asList(method.split("\n")), Arrays.asList(methodOld.split("\n")));
      return patch;
   }
}
