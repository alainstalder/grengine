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

import java.net.URL;


/**
 * URL-based script source with content considered static,
 * default implementation of the {@link UrlSource} interface.
 * <p>
 * The ID is the URL as a string.
 * <p>
 * The method {@link #getLastModified()} always returns 0.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultUrlSource extends BaseSource implements UrlSource {
    
    private final URL url;
    
    /**
     * constructor from URL.
     * 
     * @throws IllegalArgumentException if URL is null
     * 
     * @since 1.0
     */
    public DefaultUrlSource(final URL url) {
        if (url == null) {
            throw new IllegalArgumentException("URL is null.");
        }
        id = url.toString();
        this.url = url;
    }
    
    @Override
    public URL getUrl() {
        return url;
    }

}
