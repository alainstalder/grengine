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

package ch.grengine.source;

import java.io.File;
import java.net.URL;


/**
 * Source factory.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface SourceFactory {
    
    /**
     * gets source from script text.
     * 
     * @since 1.0
     */
    Source fromText(String text);
    
    /**
     * gets source from script text and desired class name.
     * 
     * @since 1.0
     */
    Source fromText(String text, String desiredClassName);
    
    /**
     * gets source from script file.
     * 
     * @since 1.0
     */
    Source fromFile(File file);
    
    /**
     * gets source from script URL.
     * 
     * @since 1.0
     */
    Source fromUrl(URL url);

}
