package de.dagere.peass.dependencytests;

import java.io.IOException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import de.dagere.peass.vcs.GitUtils;

/**
 * Helps faking the call to git; test needs to be executed with powerMockRunner and prepared accordingly
 * @author reichelt
 *
 */
public class FakeGitUtil {
   public static void prepareGitUtils() throws IOException, InterruptedException {
      PowerMockito.mockStatic(GitUtils.class);
      PowerMockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            return null;
         }
      }).when(GitUtils.class);
      GitUtils.reset(Mockito.any());

      PowerMockito.doCallRealMethod().when(GitUtils.class);
      GitUtils.getDiff(Mockito.any(), Mockito.any());
   }
}
