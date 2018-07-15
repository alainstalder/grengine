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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;


/**
 * Static utility methods around {@link Source}.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class SourceUtil {
    
    /**
     * UTF-8 character set
     * 
     * @since 1.0
     */
    public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

    /**
     * creates a source set from the given collections of script texts,
     * using the default source factory.
     *
     * @param texts script texts
     * 
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> textsToSourceSet(final Collection<String> texts) {
        return textsToSourceSet(new DefaultSourceFactory(), texts);
    }

    /**
     * creates a source set from the given script texts,
     * using the default source factory.
     *
     * @param texts script texts
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> textsToSourceSet(final String... texts) {
        return textsToSourceSet(new DefaultSourceFactory(), texts);
    }

    /**
     * creates a source set from the given collections of script texts,
     * using the given source factory.
     *
     * @param sourceFactory source factory
     * @param texts script texts
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> textsToSourceSet(final SourceFactory sourceFactory, final Collection<String> texts) {
        Set<Source> sources = new HashSet<>();
        for (String text : texts) {
            sources.add(sourceFactory.fromText(text));
        }
        return sources;
    }
    
    /**
     * creates a source set from the given script texts,
     * using the given source factory.
     *
     * @param sourceFactory source factory
     * @param texts script texts
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> textsToSourceSet(final SourceFactory sourceFactory, final String... texts) {
        return textsToSourceSet(sourceFactory, Arrays.asList(texts));
    }
    
    /**
     * creates a source set from the given map of desired class name to script text,
     * using the default source factory.
     *
     * @param texts map of desired class name to script text
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> textsToSourceSet(final Map<String,String> texts) {
        return textsToSourceSet(new DefaultSourceFactory(), texts);
    }
    
    /**
     * creates a source set from the given map of of desired class name to script text,
     * using the given source factory.
     *
     * @param sourceFactory source factory
     * @param texts map of desired class name to script text
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> textsToSourceSet(final SourceFactory sourceFactory, final Map<String,String> texts) {
        Set<Source> sources = new HashSet<>();
        for (Entry<String,String> entry : texts.entrySet()) {
            sources.add(sourceFactory.fromText(entry.getValue(), entry.getKey()));
        }
        return sources;
    }

    
    /**
     * creates a source set from the given collections of script files,
     * using the default source factory.
     *
     * @param files script files
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> filesToSourceSet(final Collection<File> files) {
        return filesToSourceSet(new DefaultSourceFactory(), files);
    }
    
    /**
     * creates a source set from the given script files,
     * using the default source factory.
     *
     * @param files script files
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> filesToSourceSet(final File... files) {
        return filesToSourceSet(new DefaultSourceFactory(), files);
    }
    
    /**
     * creates a source set from the given collections of script files,
     * using the given source factory.
     *
     * @param sourceFactory source factory
     * @param files script files
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> filesToSourceSet(final SourceFactory sourceFactory, final Collection<File> files) {
        Set<Source> sources = new HashSet<>();
        for (File file : files) {
            sources.add(sourceFactory.fromFile(file));
        }
        return sources;
    }
    
    /**
     * creates a source set from the given script files,
     * using the given source factory.
     *
     * @param sourceFactory source factory
     * @param files script files
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> filesToSourceSet(final SourceFactory sourceFactory, final File... files) {
        return filesToSourceSet(sourceFactory, Arrays.asList(files));
    }
    
    
    /**
     * creates a source set from the given collections of script URLs,
     * using the default source factory.
     *
     * @param urls script URLs
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> urlsToSourceSet(final Collection<URL> urls) {
        return urlsToSourceSet(new DefaultSourceFactory(), urls);
    }
    
    /**
     * creates a source set from the given script URLs,
     * using the default source factory.
     *
     * @param urls script URLs
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> urlsToSourceSet(final URL... urls) {
        return urlsToSourceSet(new DefaultSourceFactory(), urls);
    }
    
    /**
     * creates a source set from the given collections of script URLs,
     * using the given source factory.
     *
     * @param sourceFactory source factory
     * @param urls script URLs
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> urlsToSourceSet(final SourceFactory sourceFactory, final Collection<URL> urls) {
        Set<Source> sources = new HashSet<>();
        for (URL url : urls) {
            sources.add(sourceFactory.fromUrl(url));
        }
        return sources;
    }
    
    /**
     * creates a source set from the given script URLs,
     * using the given source factory.
     *
     * @param sourceFactory source factory
     * @param urls script URLs
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> urlsToSourceSet(final SourceFactory sourceFactory, final URL... urls) {
        return urlsToSourceSet(sourceFactory, Arrays.asList(urls));
    }
    
    
    /**
     * converts the given source to a set of source that contains only the given source.
     *
     * @param source source
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> sourceToSourceSet(final Source source) {
        Set<Source> sourceSet = new HashSet<>();
        sourceSet.add(source);
        return sourceSet;
    }

    /**
     * converts the given collection of source to a set of source.
     *
     * @param sourceCollection source collection
     *
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> sourceCollectionToSourceSet(final Collection<Source> sourceCollection) {
        return new HashSet<>(sourceCollection);
    }

    /**
     * converts the given source varargs or array of source to a set of source.
     *
     * @param sourceArray source varargs or array of source
     * @return source set
     *
     * @since 1.0
     */
    public static Set<Source> sourceArrayToSourceSet(final Source... sourceArray) {
        Set<Source> sourceSet = new HashSet<>();
        Collections.addAll(sourceSet, sourceArray);
        return sourceSet;
    }

    
    /**
     * calculates a cryptographic hash function (message digest).
     * <p>
     * The given text is first UTF-8 encoded to bytes, then the given hash
     * is calculated and finally returned as a hex string.
     * 
     * @param text the text to hash
     * @param algorithm the hash algorithm to use
     *
     * @return hash hex string
     * @throws UnsupportedOperationException if the given hash algorithm is not available
     * 
     * @since 1.0
     */
    public static String hash(final String text, final String algorithm) {
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("No message digest " + algorithm + ".", e);
        }
        byte[] digestBytes = hash.digest(text.getBytes(CHARSET_UTF_8));
        return bytesToHex(digestBytes);
    }

    /**
     * calculates an MD5 hash.
     * <p>
     * The given text is first UTF-8 encoded to bytes, then the MD5 hash
     * is calculated and finally returned as a hex string.
     * 
     * @param text the text to hash
     *
     * @return MD5 hash text string
     * @throws UnsupportedOperationException if MD5 is not available (which is very unlikely)
     * 
     * @since 1.0
     */
    public static String md5(final String text) {
        return hash(text, "MD5");
    }

    /**
     * gets the start of the given text with line breaks removed.
     * <p>
     * The returned text is at most maxLen characters long and line breaks
     * are converted to "%n". If the text had to be cut, this is indicated
     * by "[..]" at the end of the returned text.
     * 
     * @param text text
     * @param maxLen maximal length of the returned text
     *
     * @return start of the given text with line breaks removed
     *
     * @throws IllegalArgumentException if maxLen is less than 10
     * 
     * @since 1.1.1
     */
    public static String getTextStartNoLineBreaks(final String text, final int maxLen) {
        if (text == null) {
            return null;
        }
        if (maxLen < 0) {
            throw new IllegalArgumentException("Max len (" + maxLen + ") is negative.");
        }
        if (maxLen < 10) {
            throw new IllegalArgumentException("Max len (" + maxLen + ") must be at least 10.");
        }
        String out = text;
        // reduce length to reduce processing, but keep longer than max
        if (out.length() > maxLen) {
            out = out.substring(0, maxLen + 1);
        }
        out = out.replaceAll("\r\n", "%n").replaceAll("\r", "%n").replaceAll("\n", "%n");
        if (out.length() <= maxLen) {
            return out;
        }
        return out.substring(0, maxLen - 4) + "[..]";
    }

    /**
     * gets the start of the given text with line breaks removed -
     * deprecated due to typo in method name, use
     * {@link #getTextStartNoLineBreaks(String, int)} instead.
     * <p>
     * The returned text is at most maxLen characters long and line breaks
     * are converted to "%n". If the text had to be cut, this is indicated
     * by "[..]" at the end of the returned text.
     *
     * @param text text
     * @param maxLen maximal length of the returned text
     *
     * @return start of the given text with line breaks removed
     *
     * @throws IllegalArgumentException if maxLen is less than 10
     *
     * @since 1.0
     */
    @Deprecated
    public static String getTextStartNoLinebreaks(final String text, final int maxLen) {
        return getTextStartNoLineBreaks(text, maxLen);
    }

    /**
     * reads the content from the given URL using the given character encoding.
     *
     * @param url URL
     * @param encoding encoding, e.g. "UTF-8"
     *
     * @return URL content text
     * @throws IOException if could not read from the URL
     *
     * @since 1.0
     */
    public static String readUrlText(final URL url, final String encoding) throws IOException {
        InputStream in;
        try {
            in = url.openStream();
        } catch (IOException e) {
            throw new IOException("Could not open stream for URL '" + url + "': " + e, e);
        }

        Scanner scanner = new Scanner(in, encoding);
        scanner.useDelimiter("\\A");
        String text = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        IOException e = scanner.ioException();
        if (e != null) {
            throw new IOException("Could not read from URL '" + url + "': " + e, e);
        }

        return text;
    }
    
    /**
     * converts the given file to the canonical file, with fallback
     * to the absolute file if getting the canonical file failed.
     * <p>
     * The returned file is thus guaranteed to be an absolute file.
     *
     * @param file file
     *
     * @return canonical or absolute file
     *
     * @since 1.0
     */
    public static File toCanonicalOrAbsoluteFile(final File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }

    // converts given bytes to a hex string with upper case letters
    private static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(32);
        int digit;
        for (byte b : bytes) {
            digit = (b >> 4) & 0xF;
            builder.append(digit < 10 ? (char) ('0' + digit) : (char) ('A' - 10 + digit));
            digit = (b & 0xF);
            builder.append(digit < 10 ? (char) ('0' + digit) : (char) ('A' - 10 + digit));
        }
        return builder.toString();
    }

}
