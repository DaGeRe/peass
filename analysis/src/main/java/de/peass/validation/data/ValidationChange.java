package de.peass.validation.data;

public class ValidationChange {
   
   private boolean correct;
   private String type;
   private String explanation;

   public boolean isCorrect() {
      return correct;
   }

   public void setCorrect(final boolean correct) {
      this.correct = correct;
   }

   public String getType() {
      return type;
   }

   public void setType(final String reason) {
      this.type = reason;
   }

   public String getExplanation() {
      return explanation;
   }

   public void setExplanation(final String explanation) {
      this.explanation = explanation;
   }
}