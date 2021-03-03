package de.peass.analysis.properties;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.peass.analysis.changes.Change;

public class ChangeProperty {
   
   public enum TraceChange{
      NO_CALL_CHANGE, ADDED_CALLS, REMOVED_CALLS, BOTH, UNKNOWN;
   }
   
   private String method;
   private Map<String, Integer> addedMap = new LinkedHashMap<>();
   private Map<String, Integer> removedMap = new LinkedHashMap<>();
   private double oldTime;
   private double changePercent;
   private int calls;
   private int callsOld;
   private int affectedClasses;
   private int affectedLines;
   private int newLines;
   private int oldLines;
   private String diff;
   private boolean affectsSource = false;
   private boolean affectsTestSource = false;
   private Set<String> affectedMethods = new HashSet<>();
   private List<String> types = new LinkedList<>();
   private List<String> guessedTypes = new LinkedList<>();
   private TraceChange traceChangeType;

   public ChangeProperty() {
   }

   public ChangeProperty(final Change change) {
      setMethod(change.getMethod());
      setOldTime(change.getOldTime());
      setChangePercent(change.getChangePercent());
      setDiff(change.getDiff());
   }

   public int getNewLines() {
      return newLines;
   }

   public void setNewLines(final int newLines) {
      this.newLines = newLines;
   }

   public int getOldLines() {
      return oldLines;
   }

   public void setOldLines(final int oldLines) {
      this.oldLines = oldLines;
   }

   public double getOldTime() {
      return oldTime;
   }

   public void setOldTime(final double oldTime) {
      this.oldTime = oldTime;
   }

   public int getAffectedClasses() {
      return affectedClasses;
   }

   public void setAffectedClasses(final int affectedClasses) {
      this.affectedClasses = affectedClasses;
   }

   public int getAffectedLines() {
      return affectedLines;
   }

   public void setAffectedLines(final int affectedLines) {
      this.affectedLines = affectedLines;
   }

   public List<String> getTypes() {
      return types;
   }

   public void setTypes(final List<String> types) {
      this.types = types;
   }

   public int getCalls() {
      return calls;
   }

   public void setCalls(final int calls) {
      this.calls = calls;
   }

   public int getCallsOld() {
      return callsOld;
   }

   public void setCallsOld(final int callsOld) {
      this.callsOld = callsOld;
   }

   public boolean isAffectsSource() {
      return affectsSource;
   }

   public void setAffectsSource(final boolean affectsSource) {
      this.affectsSource = affectsSource;
   }

   public boolean isAffectsTestSource() {
      return affectsTestSource;
   }

   public void setAffectsTestSource(final boolean affectsTestSource) {
      this.affectsTestSource = affectsTestSource;
   }

   public String getDiff() {
      return diff;
   }

   public void setDiff(final String diff) {
      this.diff = diff;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(final String method) {
      this.method = method;
   }

   public Map<String, Integer> getAddedMap() {
      return addedMap;
   }

   public void setAddedMap(final Map<String, Integer> addedMap) {
      this.addedMap = addedMap;
   }

   public Map<String, Integer> getRemovedMap() {
      return removedMap;
   }

   public void setRemovedMap(final Map<String, Integer> removedMap) {
      this.removedMap = removedMap;
   }

   public double getChangePercent() {
      return changePercent;
   }

   public void setChangePercent(final double changePercent) {
      this.changePercent = changePercent;
   }

   public TraceChange getTraceChangeType() {
      return traceChangeType;
   }

   public void setTraceChangeType(final TraceChange traceChangeType) {
      this.traceChangeType = traceChangeType;
   }

   public List<String> getGuessedTypes() {
      return guessedTypes;
   }

   public void setGuessedTypes(final List<String> guessedTypes) {
      this.guessedTypes = guessedTypes;
   }

   public Set<String> getAffectedMethods() {
      return affectedMethods;
   }

   public void setAffectedMethods(final Set<String> affectedMethods) {
      this.affectedMethods = affectedMethods;
   }
}