/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.except;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class CreateExceptionTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromMessage() {
        String msg = "Something.";
        CreateException e = new CreateException(msg);
        assertTrue(e instanceof GrengineException);
        assertEquals(msg, e.getMessage());
        assertNull(e.getCause());
        assertTrue(e.getDateThrown().getTime() <= System.currentTimeMillis());
        assertTrue(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis());
    }
    
    @Test
    public void testConstructFromMessageAndThrowable() {
        String msg = "Something.";
        RuntimeException cause = new RuntimeException();
        CreateException e = new CreateException(msg, cause);
        assertTrue(e instanceof GrengineException);
        assertEquals(msg + " Cause: " + cause, e.getMessage());
        assertEquals(cause, e.getCause());
        assertTrue(e.getDateThrown().getTime() <= System.currentTimeMillis());
        assertTrue(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis());
    }

    @Test
    public void testConstructFromMessageAndThrowableNull() {
        String msg = "Something.";
        CreateException e = new CreateException(msg, null);
        assertTrue(e instanceof GrengineException);
        assertEquals(msg, e.getMessage());
        assertNull(e.getCause());
        assertTrue(e.getDateThrown().getTime() <= System.currentTimeMillis());
        assertTrue(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis());
    }

}
