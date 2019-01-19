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

package ch.artecat.grengine.except;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


class GrengineExceptionTest {

    @Test
    void testConstructFromMessage() {

        // given

        final String msg = "Something.";

        // when

        final GrengineException e = new GrengineException(msg);

        // then

        assertThat(e.getMessage(), is(msg));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }
    
    @Test
    void testConstructFromMessageAndThrowable() {

        // given

        final String msg = "Something.";
        final Throwable cause = new RuntimeException();

        // when

        final GrengineException e = new GrengineException(msg, cause);

        // then

        assertThat(e.getMessage(), is(msg + " Cause: " + cause));
        assertThat(e.getCause(), is(cause));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

    @Test
    void testConstructFromMessageAndThrowableNull() {

        // given

        final String msg = "Something.";

        // when

        final GrengineException e = new GrengineException(msg, null);

        // then

        assertThat(e.getMessage(), is(msg));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

}
