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

/**
 * Interface for releasing class metadata when done using it.
 * <p>
 * Allows to remove metadata associated by Groovy (or Java) with a class,
 * which is often necessary to get on-the-fly garbage collection.
 * 
 * @since 1.1
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface ClassReleaser {

    /**
     * release class metadata when done using it.
     * <p>
     * Allows to remove metadata associated by Groovy (or Java) with a class,
     * which is often necessary to get on-the-fly garbage collection.
     *
     * @param clazz The class to release.
     *
     * @since 1.1
     */
    void release(Class<?> clazz);

}
