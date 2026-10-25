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

import ch.grengine.source.MockSource;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.deserializeObject;
import static ch.grengine.TestUtil.serializeObject;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;


class CompileExceptionTest {

    @Test
    void testConstructFromMessage() {

        // given

        final String msg = "Something.";
        final Sources s = SourcesUtil.sourceToSources(new MockSource("id1"));

        // when

        final CompileException e = new CompileException(msg, s);

        // then

        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg));
        assertThat(e.getSources(), is(s));
        assertThat(e.getSourcesAsString(), is(s.toString()));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

    // in normal use sources are not null, but covering
    @Test
    void testConstructFromMessage_sourcesNull() {

        // given

        final String msg = "Something.";

        // when

        final CompileException e = new CompileException(msg, null);

        // then

        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg));
        assertThat(e.getSources(), is(nullValue()));
        assertThat(e.getSourcesAsString(), is(""));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

    @Test
    void testConstructFromMessageAndThrowable() {

        // given

        final String msg = "Something.";
        final Throwable cause = new RuntimeException();
        final Sources s = SourcesUtil.sourceToSources(new MockSource("id1"));

        // when

        final CompileException e = new CompileException(msg, cause, s);

        // then

        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg + " Cause: " + cause));
        assertThat(e.getCause(), is(cause));
        assertThat(e.getSources(), is(s));
        assertThat(e.getSourcesAsString(), is(s.toString()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

    // in normal use sources are not null, but covering
    @Test
    void testConstructFromMessageAndThrowable_sourcesNull() {

        // given

        final String msg = "Something.";
        final Throwable cause = new RuntimeException();

        // when

        final CompileException e = new CompileException(msg, cause, null);

        // then

        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg + " Cause: " + cause));
        assertThat(e.getCause(), is(cause));
        assertThat(e.getSources(), is(nullValue()));
        assertThat(e.getSourcesAsString(), is(""));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

    @Test
    void testConstructFromMessageAndThrowableNull() {

        // given

        final String msg = "Something.";
        final Sources s = SourcesUtil.sourceToSources(new MockSource("id1"));

        // when

        final CompileException e = new CompileException(msg, null, s);

        // then

        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is(msg));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getSources(), is(s));
        assertThat(e.getSourcesAsString(), is(s.toString()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

    @Test
    void testSerializeDeserialize() {

        // given

        final String msg = "Something.";
        final Throwable cause = new RuntimeException("out-of-the-blue");
        final Sources s = SourcesUtil.sourceToSources(new MockSource("id1"));

        // when

        final CompileException e = new CompileException(msg, cause, s);
        final String eSerialized = serializeObject(e);
        final Object eDeserialized = deserializeObject(eSerialized);

        // then

        assertThat(eDeserialized, instanceOf(CompileException.class));
        CompileException e2 = (CompileException)eDeserialized;
        assertThat(e2.getMessage(), is(msg + " Cause: " + cause));
        assertThat(e2.getCause(), instanceOf(RuntimeException.class));
        assertThat(e2.getCause(), is(not(e.getCause())));
        assertThat(e2.getCause().getMessage(), is("out-of-the-blue"));
        assertThat(e2.getSources(), is(nullValue()));
        assertThat(e2.getSourcesAsString(), is(s.toString()));
        assertThat(e2.getDateThrown(), is(e.getDateThrown()));
    }

}
