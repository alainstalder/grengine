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

package ch.grengine.sources;

import ch.grengine.code.CompilerFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceSetState;

import java.util.Set;


/**
 * Abstract base class for implementing {@link Sources}.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public abstract class BaseSources implements Sources {

    private String name;
    private CompilerFactory compilerFactory;
    private long latencyMs;
    private volatile SourceSetState state;

    /**
     * constructor.
     * 
     * @since 1.0
     */
    public BaseSources() {
    }
    
    /**
     * initialize.
     * 
     * @param name sources name
     * @param compilerFactory compiler factory
     * @param latencyMs latency in milliseconds for updating
     * 
     * @since 1.0
     */
    public void init(final String name, final CompilerFactory compilerFactory, final long latencyMs) {
        this.name = name;
        this.compilerFactory = compilerFactory;
        this.latencyMs = latencyMs;
        state = new SourceSetState(getSourceSetNew());
    }
    
    /**
     * gets the updated source set.
     *
     * @return updated source set
     * 
     * @since 1.0
     */
    protected abstract Set<Source> getSourceSetNew();
        
    @Override
    public Set<Source> getSourceSet() {
        
        SourceSetState stateNow = state;
        
        // check both boundaries of the interval to exclude problems with leap seconds etc.
        long diff = System.currentTimeMillis() - stateNow.getLastChecked();
        if (diff >= 0 && diff < latencyMs) {
            return stateNow.getSourceSet();
        }
        
        synchronized(this) {
            // prevent multiple updates
            stateNow = state;
            diff = System.currentTimeMillis() - stateNow.getLastChecked();
            if (diff >= 0 && diff < latencyMs) {
                return stateNow.getSourceSet();
            }
            state = stateNow.update(getSourceSetNew());
            return state.getSourceSet();
        }
    }
    
    @Override
    public long getLastModified() {
        getSourceSet();
        return state.getLastModified();
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompilerFactory getCompilerFactory() {
        return compilerFactory;
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[name='" + getName() + "']";
    }
    
}