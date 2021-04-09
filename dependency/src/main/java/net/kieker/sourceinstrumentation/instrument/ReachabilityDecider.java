package net.kieker.sourceinstrumentation.instrument;

import java.util.Optional;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;

public class ReachabilityDecider {
   public static boolean isAfterUnreachable(final BlockStmt originalBlock) {
      boolean afterUnreachable = false;
      Optional<Statement> optionalLast = originalBlock.getStatements().getLast();
      if (optionalLast.isPresent()) {
         Statement last = optionalLast.get();
         if (last instanceof ThrowStmt) {
            afterUnreachable = true;
         } else if (last instanceof WhileStmt) {
            WhileStmt stmt = (WhileStmt) last;
            if (stmt.getCondition().toString().equals("true")) {
               afterUnreachable = true;
            }
         } else if (last instanceof DoStmt) {
            DoStmt stmt = (DoStmt) last;
            if (stmt.getCondition().toString().equals("true")) {
               afterUnreachable = true;
            }
         } else if (last instanceof TryStmt) {
            TryStmt stmt = (TryStmt) last;
            afterUnreachable = isAfterUnreachable(stmt.getTryBlock());
            if (afterUnreachable) {
               for (CatchClause catchClause : stmt.getCatchClauses()) {
                  afterUnreachable &= isAfterUnreachable(catchClause.getBody());
               }
            }
         } else if (last instanceof BlockStmt) {
            return isAfterUnreachable((BlockStmt) last);
         }
      }
      return afterUnreachable;
   }

}
