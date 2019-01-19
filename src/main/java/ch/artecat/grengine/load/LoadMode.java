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

package ch.artecat.grengine.load;



/**
 * Mode for loading classes.
 * <p>
 * The traditional way to load classes in the Java VM is "parent first",
 * while "current first" is a newer approach to load classes, which, for example,
 * servlet containers or similar applications use in some cases.
 * Both approaches have advantages and disadvantages.
 * <p>
 * The "parent first" approach is maybe often better in more static situations,
 * while "current first" is maybe often better in more dynamic situations,
 * like maybe also often with dynamically compiled Groovy scripts.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public enum LoadMode {
    
    /**
     * tries to load from the parent class loader first,
     * then (if not found) from the current class loader.
     * 
     * @since 1.0
     */
    PARENT_FIRST,

    /**
     * tries to load from the current class loader first,
     * then (if not found) from the parent class loader.
     * 
     * @since 1.0
     */
    CURRENT_FIRST

}

