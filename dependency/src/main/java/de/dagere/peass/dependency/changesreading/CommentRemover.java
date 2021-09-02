package de.dagere.peass.dependency.changesreading;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;

class CommentRemover{
   
   private static final Logger LOG = LogManager.getLogger(CommentRemover.class);
   
   public CommentRemover(final Node node) {
      clearComments(node);
   }

   private void clearComments(final Node node) {
      clearOuterComment(node);
      List<Comment> comments = new LinkedList<>(node.getAllContainedComments());
      for (Comment comment : comments) {
         node.removeOrphanComment(comment);
      }
      final List<Node> childNodes = node.getChildNodes();
      LOG.trace("Type: {}", childNodes.getClass());
      clearInnerComments(childNodes);
   }

   private void clearOuterComment(final Node node) {
      final Optional<Comment> comment = node.getComment();
      if (comment.isPresent()) {
         final Comment realComment = comment.get();
         if (realComment.getComment() != null) {
            realComment.setContent("");
         }
         node.setComment(null);
      }
   }

   private void clearInnerComments(final List<Node> childNodes) {
      for (final Iterator<Node> begin = childNodes.iterator(); begin.hasNext();) {
         final Node child = begin.next();
         if (child instanceof LineComment) {
            ((LineComment) child).setContent("");
         } else if (child instanceof JavadocComment) {
            ((JavadocComment) child).setContent("");
         } else {
            clearComments(child);
         }
      }
   }
}