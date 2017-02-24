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


/**
 * Text-based script source, default implementation of the {@link TextSource} interface.
 * <p>
 * The source ID is {@literal "/groovy/script/Script<text-hash>" resp.
 * "/groovy/script/<text-hash>/<desired-class-name>"}.
 * <p>
 * The text hash is calculated as follows:
 * <ul>
 * <li>UTF-8 encode the script text to bytes
 * <li>calculate the MD5 hash
 * <li>convert the resulting bytes to a hex string
 * </ul>
 * <p>
 * The method {@link #getLastModified()} always returns 0.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultTextSource extends BaseSource implements TextSource {
    
    private final String text;
    
    /**
     * constructor from script text.
     * <p>
     * If the script text does not explicitly declare a class, the name
     * of the compiled script class will normally be {@literal "Script<text-hash>"}.
     *
     * @param text script text
     *
     * @throws IllegalArgumentException if text is null
     * 
     * @since 1.0
     */
    public DefaultTextSource(final String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text is null.");
        }
        id = "/groovy/script/Script" + SourceUtil.md5(text);
        this.text = text;
    }

    /**
     * constructor from script text and desired class name.
     * <p>
     * If the script text does not explicitly declare a class, the name
     * of the compiled script class will normally be the given desired class name.
     *
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @throws IllegalArgumentException if text or desired class name is null
     * 
     * @since 1.0
     */
    public DefaultTextSource(final String text, final String desiredClassName) {
        if (text == null) {
            throw new IllegalArgumentException("Text is null.");
        }
        if (desiredClassName == null) {
            throw new IllegalArgumentException("Desired class name is null.");
        }
        id = "/groovy/script/Script" + SourceUtil.md5(text) + "/" + desiredClassName;
        this.text = text;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[ID=" + getId() +
                ", text='" + SourceUtil.getTextStartNoLineBreaks(getText(), 200) + "']";
    }
    
    @Override
    public String getText() {
        return text;
    }

}
