package de.peran.analysis.helper.read;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.javaparser.ast.CompilationUnit;

import de.peass.analysis.changes.Change;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.ChangeProperty.TraceChange;
import de.peass.dependency.ChangeManager;
import de.peass.dependency.ClazzFinder;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.FileComparisonUtil;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.ClazzChangeData;
import de.peass.dependency.analysis.data.VersionDiff;
import de.peass.dependency.execution.MavenPomUtil;
import de.peass.dependency.traces.requitur.Sequitur;
import de.peass.vcs.GitUtils;
import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import difflib.Patch;

public class PropertyReadHelper {

   private static final Logger LOG = LogManager.getLogger(PropertyReadHelper.class);

   public static final String keywords[] = { "abstract", "assert", "boolean",
         "break", "byte", "case", "catch", "char", "class", "const",
         "continue", "default", "do", "double", "else", "extends", "false",
         "final", "finally", "float", "for", "goto", "if", "implements",
         "import", "instanceof", "int", "interface", "long", "native",
         "new", "null", "package", "private", "protected", "public",
         "return", "short", "static", "strictfp", "super", "switch",
         "synchronized", "this", "throw", "throws", "transient", "true",
         "try", "void", "volatile", "while",
         "System.out.println", "System.gc", "Thread.sleep" };

   private final ChangedEntity clazz;
   private final String version, prevVersion;
   private final Change change;
   private final File projectFolder;

   private final File viewFolder;

   public static void main(final String[] args) throws IOException {
      final ChangedEntity ce = new ChangedEntity("org.apache.commons.fileupload.ServletFileUploadTest", "");
      final Change change = new Change();
      change.setChangePercent(-8.0);
      change.setMethod("testFoldedHeaders");
      final File projectFolder2 = new File("../../projekte/commons-fileupload");
      final File viewFolder2 = new File("/home/reichelt/daten3/diss/repos/views-final/views_commons-fileupload/");
      final PropertyReadHelper propertyReadHelper = new PropertyReadHelper("4ed6e923cb2033272fcb993978d69e325990a5aa", "b53957", ce, change, projectFolder2, viewFolder2);
      propertyReadHelper.read();
   }

   public PropertyReadHelper(final String version, final String prevVersion, final ChangedEntity clazz, final Change change, final File projectFolder, final File viewFolder) {
      this.version = version;
      this.prevVersion = prevVersion;
      if (clazz.getMethod() != null) {
         throw new RuntimeException("Method must not be set!");
      }
      this.clazz = clazz;
      this.change = change;
      this.projectFolder = projectFolder;
      this.viewFolder = viewFolder;
   }

   public ChangeProperty read() throws IOException {
      final ChangeProperty property = new ChangeProperty(change);

      getSourceInfos(property);

      LOG.debug("Comparing " + version + " " + property.getMethod());
      final File folder = new File(viewFolder, "view_" + version + File.separator + clazz + File.separator + property.getMethod());
      if (folder.exists()) {
         // analyseTraceChange(folder, property);
         return property;
      } else {
         LOG.error("Folder {} does not exist", folder);
         return property;
      }
   }

   private String getShortPrevVersion() {
      if (prevVersion.endsWith("~1")) {
         return prevVersion.substring(0, 6) + "~1";
      } else {
         return prevVersion.substring(0, 6);
      }
   }

   public void getSourceInfos(final ChangeProperty property) throws FileNotFoundException, IOException {
      final File folder = new File(viewFolder, "view_" + version + File.separator + clazz + File.separator + property.getMethod());
      final File fileCurrent = new File(folder, version.substring(0, 6) + "_method");
      final File fileOld = new File(folder, getShortPrevVersion() + "_method");
      final Set<String> calls = new HashSet<>();
      if (fileCurrent.exists() && fileOld.exists()) {

         final PeASSFolders folders = new PeASSFolders(projectFolder);
         final ChangeManager changeManager = new ChangeManager(folders);
         final Map<ChangedEntity, ClazzChangeData> changes = changeManager.getChanges(prevVersion, version);

         final List<String> traceCurrent = Sequitur.getExpandedTrace(fileCurrent);
         final List<String> traceOld = Sequitur.getExpandedTrace(fileOld);
         determineTraceSizeChanges(property, traceCurrent, traceOld);

         final Set<String> intersection = new HashSet<>(traceCurrent);
         final Set<String> calledCurrent = new HashSet<>(traceCurrent);
         final Set<String> calledOld = new HashSet<>(traceOld);
         calls.addAll(calledCurrent);
         calls.addAll(calledOld);

         intersection.retainAll(calledOld);

         for (final String calledInBoth : intersection) {
            final String clazz = calledInBoth.substring(0, calledInBoth.indexOf("#"));
            final int openingParenthesis = calledInBoth.indexOf("(");
            String method;
            if (openingParenthesis != -1) {
               method = calledInBoth.substring(calledInBoth.indexOf("#") + 1, openingParenthesis);
            } else {
               method = calledInBoth.substring(calledInBoth.indexOf("#") + 1);
            }
            System.out.println(calledInBoth);

            final ChangedEntity entity = new ChangedEntity(clazz, "", method);
            final Patch<String> patch = changeManager.getKeywordChanges(entity);

            final Map<String, Integer> vNewkeywords = new HashMap<>();
            final Map<String, Integer> vOldkeywords = new HashMap<>();
            for (final Delta<String> changeSet : patch.getDeltas()) {
               for (final String line : changeSet.getOriginal().getLines()) {
                  getKeywordCount(vOldkeywords, line);
               }
               for (final String line : changeSet.getRevised().getLines()) {
                  getKeywordCount(vNewkeywords, line);
               }
            }
            for (final Map.Entry<String, Integer> vNew : vNewkeywords.entrySet()) {
               property.getAddedMap().put(vNew.getKey(), vNew.getValue());
            }
            for (final Map.Entry<String, Integer> vOld : vOldkeywords.entrySet()) {
               // System.out.println("Removed: " + v2.getKey() + " " + v2.getValue());
               property.getRemovedMap().put(vOld.getKey(), vOld.getValue());
            }

         }

         try {
            final VersionDiff diff = GitUtils.getChangedFiles(projectFolder, MavenPomUtil.getGenericModules(projectFolder), version);
            for (final Iterator<ChangedEntity> it = diff.getChangedClasses().iterator(); it.hasNext();) {
               final ChangedEntity entity = it.next();
               boolean called = false;
               for (final String call : calls) {
                  if (call.startsWith(entity.getJavaClazzName())) {
                     called = true;
                     break;
                  }
               }
               if (!called)
                  it.remove();
            }
            property.setAffectedClasses(diff.getChangedClasses().size());
            final int changedLines = GitUtils.getChangedLines(projectFolder, version, diff.getChangedClasses());
            property.setAffectedLines(changedLines);
         } catch (final XmlPullParserException e) {
            e.printStackTrace();
         }

         System.out.println("Calls: " + calls);

         getTestSourceAffection(property, calls, folders, changes);
      } else {
         if (!fileCurrent.exists()) {
            LOG.error("Not found: {}", fileCurrent);
         } else {
            LOG.error("Not found: {}", fileOld);
         }

      }

   }

   void getTestSourceAffection(final ChangeProperty property, final Set<String> calls, final PeASSFolders folders, final Map<ChangedEntity, ClazzChangeData> changes)
         throws FileNotFoundException {
      final ClazzChangeData clazzChangeData = changes.get(clazz);
      if (clazzChangeData != null) {
         for (Set<String> methodsOfClazz : clazzChangeData.getChangedMethods().values()) {
            if (methodsOfClazz.contains(property.getMethod())) {
               property.setAffectsTestSource(true);
            }
         }
      }

      // Prinzipiell: Man müsste schauen, wo der Quelltext liegt, nicht, wie er heißt..
      for (final Entry<ChangedEntity, ClazzChangeData> changedEntity : changes.entrySet()) {
         final Set<String> guessedTypes = getGuesses(folders, changedEntity);
         property.getGuessedTypes().addAll(guessedTypes);

         if (!changedEntity.getValue().isOnlyMethodChange()) {
            for (final String call : calls) {
               final String clazzCall = call.substring(0, call.indexOf("#"));
               if (changedEntity.getKey().getJavaClazzName().equals(clazzCall)) {
                  processFoundCall(property, changedEntity);
               }
            }
         } else {
            for (Map.Entry<String, Set<String>> changedClazz : changedEntity.getValue().getChangedMethods().entrySet()) {
               for (String changedMethod : changedClazz.getValue()) {
                  String fqn;
                  if (changedMethod.contains(ChangedEntity.METHOD_SEPARATOR)) {
                     fqn = changedEntity.getKey() + ChangedEntity.CLAZZ_SEPARATOR + changedMethod;
                  } else {
                     fqn = changedEntity.getKey() + ChangedEntity.METHOD_SEPARATOR + changedMethod;
                  }
                  if (calls.contains(fqn)) {
                     processFoundCall(property, changedEntity);
                  }
               }
            }
         }
      }
   }

   private Set<String> getGuesses(final PeASSFolders folders, final Entry<ChangedEntity, ClazzChangeData> changedEntity) throws FileNotFoundException {
      final Set<String> guessedTypes = new HashSet<>();
      final File file = ClazzFinder.getSourceFile(folders.getProjectFolder(), changedEntity.getKey());
      final File fileOld = ClazzFinder.getSourceFile(folders.getOldSources(), changedEntity.getKey());

      if (file != null && fileOld != null && file.exists() && fileOld.exists()) {
         final CompilationUnit clazzUnit = FileComparisonUtil.parse(file);
         final CompilationUnit clazzUnitOld = FileComparisonUtil.parse(fileOld);

         for (Map.Entry<String, Set<String>> changedClazz : changedEntity.getValue().getChangedMethods().entrySet()) {
            for (String method : changedClazz.getValue()) {
               final String source = FileComparisonUtil.getMethod(changedEntity.getKey(), method, clazzUnit);
               final String sourceOld = FileComparisonUtil.getMethod(changedEntity.getKey(), method, clazzUnitOld);
               final Patch<String> changedLinesMethod = DiffUtils.diff(Arrays.asList(sourceOld.split("\n")), Arrays.asList(source.split("\n")));

               for (final Delta<String> delta : changedLinesMethod.getDeltas()) {
                  getDeltaGuess(guessedTypes, (delta.getOriginal().getLines()));
                  getDeltaGuess(guessedTypes, (delta.getRevised().getLines()));
               }
            }
         }
      }
      return guessedTypes;
   }

   private void getDeltaGuess(final Set<String> guessedTypes, final List<String> delta) {
      for (final String line : delta) {
         if (line.contains("synchronized")) {
            guessedTypes.add("SYNCHRONIZED");
         }
         if (line.contains("toArray")) {
            guessedTypes.add("OPTIM");
         }
      }
   }

   /**
    * Determines how the trace has changed viewed from trace1, e.g. ADDED_CALLS means that trace2 has more calls than trace1.
    * 
    * @param property
    * @param traceCurrent
    * @param traceOld
    * @throws IOException
    */
   public static void determineTraceSizeChanges(final ChangeProperty property, final List<String> traceCurrent, final List<String> traceOld) throws IOException {
      LOG.debug("Trace sizes: {}, {}", traceCurrent.size(), traceOld.size());
      if (traceCurrent.size() + traceOld.size() < 10000) {
         final Patch<String> patch = DiffUtils.diff(traceOld, traceCurrent);

         System.out.println(patch);

         int added = 0, removed = 0;
         for (final Delta<String> delta : patch.getDeltas()) {
            if (delta.getType().equals(TYPE.DELETE)) {
               removed++;
            } else if (delta.getType().equals(TYPE.INSERT)) {
               added++;
            } else if (delta.getType().equals(TYPE.CHANGE)) {
               added++;
               removed++;
            }
         }

         if (added > 0 && removed > 0) {
            property.setTraceChangeType(TraceChange.BOTH);
         } else if (added > 0) {
            property.setTraceChangeType(TraceChange.ADDED_CALLS);
         } else if (removed > 0) {
            property.setTraceChangeType(TraceChange.REMOVED_CALLS);
         } else {
            property.setTraceChangeType(TraceChange.NO_CALL_CHANGE);
         }
      } else {
         property.setTraceChangeType(TraceChange.UNKNOWN);
      }

      property.setCalls(traceCurrent.size());
      property.setCallsOld(traceOld.size());
   }

   private void processFoundCall(final ChangeProperty property, final Entry<ChangedEntity, ClazzChangeData> changedEntity) {
      if (changedEntity.getKey().getClazz().toLowerCase().contains("test")) {
         property.setAffectsTestSource(true);
      } else {
         property.setAffectsSource(true);
      }
   }

   private static void getKeywordCount(final Map<String, Integer> v1keywords, final String line) {
      for (final String keyword : keywords) {
         if (line.contains(keyword)) {
            final Integer integer = v1keywords.get(keyword);
            final int count = integer != null ? integer : 0;
            v1keywords.put(keyword, count + 1);
         }
      }
   }

}
