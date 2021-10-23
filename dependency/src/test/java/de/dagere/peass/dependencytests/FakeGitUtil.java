package de.dagere.peass.dependencytests;

import java.io.IOException;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.dagere.peass.vcs.GitUtils;

/**
 * Helps faking the call to git; test needs to be executed with powerMockRunner and prepared accordingly
 * 
 * @author reichelt
 *
 */
public class FakeGitUtil {
   public static void prepareGitUtils(final MockedStatic<GitUtils> gitUtilsMock) throws IOException, InterruptedException {
      gitUtilsMock.when(() -> GitUtils.reset(Mockito.any())).then(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            return null;
         }
      });
   }
}
