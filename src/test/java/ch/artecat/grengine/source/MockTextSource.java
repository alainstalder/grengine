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

package ch.artecat.grengine.source;

// allows to set script text and last modified (thus only effective for same source instance)
public class MockTextSource extends DefaultTextSource {
    
    private volatile String text;
    private volatile long lastModified;
    private volatile RuntimeException runtimeEx;
    
    public MockTextSource(final String text) {
        super(text);
        this.text = text;
        lastModified = 0;
    }
    
    public MockTextSource(final String text, final String name) {
        super(text, name);
        lastModified = 0;
    }
    
    public void setText(final String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        if (runtimeEx != null) {
            throw runtimeEx;
        }
        return text;
    }
    
    public void setThrowAtGetText(final RuntimeException runtimeEx) {
        this.runtimeEx = runtimeEx;
    }
    
    public void setLastModified(final long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

}
