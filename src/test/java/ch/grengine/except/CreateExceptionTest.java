/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.except;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class CreateExceptionTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromMessage() {
        String msg = "Something.";
        CreateException e = new CreateException(msg);
        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }
    
    @Test
    public void testConstructFromMessageAndThrowable() {
        String msg = "Something.";
        Throwable cause = new RuntimeException();
        CreateException e = new CreateException(msg, cause);
        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg + " Cause: " + cause));
        assertThat(e.getCause(), is(cause));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

    @Test
    public void testConstructFromMessageAndThrowableNull() {
        String msg = "Something.";
        CreateException e = new CreateException(msg, null);
        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

}
