
/**
 *     This file is part of Peass.
 *
 *     Peass is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Peass is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Peass.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TestMocked {

   public final Object myMock = Mockito.mock(Object.class);
   
   @BeforeEach
   public void setUp() {
      Mockito.when(myMock.toString()).thenReturn("This is mocked!");
   }
   
   @Test
   public void testMethod1() {
      Assert.assertEquals("This is mocked!", myMock.toString());
   }
}