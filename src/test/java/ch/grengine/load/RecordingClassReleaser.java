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

package ch.grengine.load;

import java.util.HashSet;
import java.util.Set;

/**
 * Records all released classes.
 */
public class RecordingClassReleaser implements ClassReleaser {

    public final Set<Class<?>> classes = new HashSet<>();

    public boolean throwAfterReleasing;

    @Override
    public void release(Class<?> clazz) {
        classes.add(clazz);
        if (throwAfterReleasing) {
            throw new RuntimeException(clazz.getName());
        }
    }

    public int countClassesWithName(String className) {
        int count = 0;
        for (Class<?> clazz : classes) {
            if (className.equals(clazz.getName())) {
                count++;
            }
        }
        return count;
    }
}
