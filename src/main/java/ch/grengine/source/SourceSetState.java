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

package ch.grengine.source;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Helper class for tracking updates of a set of {@link Source}.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class SourceSetState {
    
    private final Set<Source> sourceSet;
    private final Map<Source,Long> lastModifiedMap;
    private final long lastChecked;
    private final long lastModified;
    
    private SourceSetState(final Set<Source> sourceSet, final Map<Source,Long> lastModifiedMap,
            final long lastChecked, final long lastModified) {
        this.sourceSet = sourceSet;
        this.lastModifiedMap = lastModifiedMap;
        this.lastChecked = lastChecked;
        this.lastModified = lastModified;
    }
    
    /**
     * constructor from source set.
     *
     * @param sourceSet source set
     *
     * @throws IllegalArgumentException if the source set is null
     * 
     * @since 1.0
     */
    public SourceSetState(final Set<Source> sourceSet) {
        if (sourceSet == null) {
            throw new IllegalArgumentException("Source set is null.");
        }
        this.sourceSet = sourceSet;
        this.lastModifiedMap = getLastModifiedMap(sourceSet);
        long now = System.currentTimeMillis();
        lastChecked = now;
        lastModified = now;
    }
    
    /**
     * gets the source set.
     *
     * @return source set
     * 
     * @since 1.0
     */
    public Set<Source> getSourceSet() {
        return sourceSet;
    }

    /**
     * gets last checked.
     *
     * @return last checked
     * 
     * @since 1.0
     */
    public long getLastChecked() {
        return lastChecked;
    }

    /**
     * gets last modified.
     *
     * @return last modified
     * 
     * @since 1.0
     */
    public long getLastModified() {
        return lastModified;
    }
    
    /**
     * returns a new instance with given source set and updated last checked and last modified.
     *
     * @param sourceSetNew source set
     * 
     * @return new instance
     * @throws IllegalArgumentException if the new source set is null
     * 
     * @since 1.0
     */
    public SourceSetState update(final Set<Source> sourceSetNew) {
        if (sourceSetNew == null) {
            throw new IllegalArgumentException("New source set is null.");
        }
        
        Map<Source,Long> lastModifiedMapNew = getLastModifiedMap(sourceSetNew);

        boolean hasChanged = true;
        if (sourceSetNew.equals(sourceSet)) {
            hasChanged = sourceSetNew.stream()
                    .anyMatch(source -> lastModifiedMap.get(source).longValue() != lastModifiedMapNew.get(source));
        }
        long lastCheckedNew = System.currentTimeMillis();
        long lastModifiedNew = hasChanged ? lastCheckedNew : lastModified;
        return new SourceSetState(sourceSetNew, lastModifiedMapNew, lastCheckedNew, lastModifiedNew);
    }
    
    private static Map<Source,Long> getLastModifiedMap(final Set<Source> sourceSet) {
        return sourceSet.stream()
                .collect(Collectors.toMap(source -> source, Source::getLastModified));
    }

}
