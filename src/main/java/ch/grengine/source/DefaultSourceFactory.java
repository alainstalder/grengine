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
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Default implementation of the {@link SourceFactory} interface.
 * <p>
 * Generally used by default within Grengine, unless a different
 * source factory has been explicitly indicated.
 * <p>
 * If the builder's URL content tracking is set, {@link Source#getLastModified()}
 * will be updated based on the URL content, within the latency period
 * and only if queried. For this purpose, a hash of the URL content is cached
 * internally for each URL source.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultSourceFactory implements SourceFactory {
            
    private final Builder builder;
    
    private final boolean trackTextSourceId;
    // key is script text, value is ID of text source
    private final Map<String,String> textSourceIdTrackingMap;
    
    private final boolean trackFileSourceLastModified;
    // key is file id, value is file last modified
    private final Map<String,Long> fileLastModifiedTrackingMap;
    private final long fileLastModifiedLatencyMs;
    private volatile long fileLastModifiedLastChecked;
    
    private final boolean trackUrlContent;
    private final long urlTrackingLatencyMs;
    private final Map<Source,TrackingInfo> urlContentTrackingMap;
    
    /**
     * constructor from builder.
     *
     * @param builder builder
     * 
     * @since 1.0
     */
    protected DefaultSourceFactory(final Builder builder) {
        this.builder = builder.commit();

        trackTextSourceId = builder.isTrackTextSourceIds();
        textSourceIdTrackingMap = new ConcurrentHashMap<String,String>();
        
        trackFileSourceLastModified = builder.isTrackFileSourceLastModified();
        fileLastModifiedTrackingMap = new ConcurrentHashMap<String,Long>();
        fileLastModifiedLatencyMs = builder.getFileLastModifiedTrackingLatencyMs();
        fileLastModifiedLastChecked = 0;
        
        trackUrlContent = builder.isTrackUrlContent();
        urlTrackingLatencyMs = builder.getUrlTrackingLatencyMs();
        urlContentTrackingMap = new ConcurrentHashMap<Source,TrackingInfo>();
    }

    /**
     * constructor from default builder (no URL tracking).
     * 
     * @since 1.0
     */
    public DefaultSourceFactory() {
        this(new Builder());
    }

    @Override
    public Source fromText(final String text) {
        if (trackTextSourceId) {
            return new SourceIdTrackingTextSource(text);
        } else {
            return new DefaultTextSource(text);
        }
    }

    @Override
    public Source fromText(final String text, final String desiredClassName) {
        if (trackTextSourceId) {
            return new SourceIdTrackingTextSource(text, desiredClassName);
        } else {
            return new DefaultTextSource(text, desiredClassName);
        }
    }

    @Override
    public Source fromFile(final File file) {
        if (trackFileSourceLastModified) {
            return new LastModifiedTrackingFileSource(file);
        } else {
            return new DefaultFileSource(file);
        }
    }

    @Override
    public Source fromUrl(final URL url) {
        if (trackUrlContent) {
            return new ContentTrackingUrlSource(url);
        } else {
            return new DefaultUrlSource(url);
        }
    }
    
    /**
     * clears the cache of URL content hashes.
     * 
     * @since 1.0
     */
    public void clearCache() {
        urlContentTrackingMap.clear();
    }
    
    /**
     * gets the builder.
     *
     * @return builder
     * 
     * @since 1.0
     */
    public Builder getBuilder() {
        return builder;
    }

    
    class SourceIdTrackingTextSource extends BaseSource implements TextSource {
        
        private final String text;
        
        public SourceIdTrackingTextSource(final String text) {
            if (text == null) {
                throw new IllegalArgumentException("Text is null.");
            }
            id = textSourceIdTrackingMap.get(text);
            if (id == null) {
                id = "/groovy/script/Script" + SourceUtil.md5(text);
                textSourceIdTrackingMap.put(text, id);
            }
            this.text = text;
        }

        public SourceIdTrackingTextSource(final String text, final String desiredClassName) {
            if (text == null) {
                throw new IllegalArgumentException("Text is null.");
            }
            if (desiredClassName == null) {
                throw new IllegalArgumentException("Desired class name is null.");
            }
            id = textSourceIdTrackingMap.get(text);
            if (id == null) {
                id = "/groovy/script/Script" + SourceUtil.md5(text);
                textSourceIdTrackingMap.put(text, id);
            }
            id += "/" + desiredClassName;
            this.text = text;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[ID=" + getId() +
                    ", text='" + SourceUtil.getTextStartNoLinebreaks(getText(), 200) + "']";
        }
        
        @Override
        public String getText() {
            return text;
        }

    }
    
    
    private long getFileSourceLastModified(File file, String id) {
        Long lastMod = fileLastModifiedTrackingMap.get(id);
        if (lastMod == null) {
            lastMod = file.lastModified();
            fileLastModifiedTrackingMap.put(id, lastMod);
            fileLastModifiedLastChecked = System.currentTimeMillis();
            return lastMod;
        }
        // check both boundaries of the interval to exclude problems with leap seconds etc.
        long diff = System.currentTimeMillis() - fileLastModifiedLastChecked;
        if (fileLastModifiedLastChecked != 0 && diff >= 0 && diff < fileLastModifiedLatencyMs) {
            return lastMod;
        }
        lastMod = file.lastModified();
        fileLastModifiedTrackingMap.put(id, lastMod);
        fileLastModifiedLastChecked = System.currentTimeMillis();
        return lastMod;
    }
    
    class LastModifiedTrackingFileSource extends DefaultFileSource {
        
        public LastModifiedTrackingFileSource(File file) {
            super(file);
        }
        
        @Override
        public long getLastModified() {
            return getFileSourceLastModified(getFile(), id);
        }

    }

    
    class ContentTrackingUrlSource extends DefaultUrlSource {
        
        private ContentTrackingUrlSource(URL url) {
            super(url);
        }
        
        private String getTextHash() {
            try {
                return SourceUtil.md5(SourceUtil.readUrlText(getUrl(), "UTF-8"));
            } catch (IOException e) {
                return "could-not-read-url-text";
            }
        }
        
        @Override
        public long getLastModified() {

            TrackingInfo info = urlContentTrackingMap.get(this);
            if (info != null) {
                // check both boundaries of the interval to exclude problems with leap seconds etc.
                long diff = System.currentTimeMillis() - info.lastChecked;
                if (diff >= 0 && diff < urlTrackingLatencyMs) {
                    return info.lastModified;
                }
            }

            synchronized(this) {
                // prevent multiple updates
                info = urlContentTrackingMap.get(this);
                if (info != null) {
                    long diff = System.currentTimeMillis() - info.lastChecked;
                    if (diff >= 0 && diff < urlTrackingLatencyMs) {
                        return info.lastModified;
                    }
                }
                
                String textHashNew = getTextHash();
                long now = System.currentTimeMillis();
                if (info != null && info.textHash.equals(textHashNew)) {
                    urlContentTrackingMap.put(this, new TrackingInfo(now, info.lastModified, info.textHash));
                    return info.lastModified;
                } else {
                    urlContentTrackingMap.put(this, new TrackingInfo(now, now, textHashNew));
                    return now;
                }
            }
        }
        
    }
    
    private class TrackingInfo {
        final long lastChecked;
        final long lastModified;
        final String textHash;
        private TrackingInfo(final long lastChecked, final long lastModified, final String textHash) {
            this.lastChecked = lastChecked;
            this.lastModified = lastModified;
            this.textHash = textHash;
        }
    }
    
    
    /**
     * Builder for {@link SourceFactory} instances.
     * 
     * @since 1.0
     * 
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Builder {
        
        /**
         * default latency for tracking URL content (60000ms = one minute).
         * 
         * @since 1.0
         */
        public static final long DEFAULT_URL_TRACKING_LATENCY_MS = 60000L;
        public static final long DEFAULT_FILE_LAST_MODIFIED_TRACKING_LATENCY_MS = 1000L;
        
        private boolean isCommitted;
        
        private boolean trackTextSourceIds = false;
        private boolean trackFileSourceLastModified = false;
        private long fileLastModifiedTrackingLatencyMs = -1;
        private boolean trackUrlContent = false;
        private long urlTrackingLatencyMs = -1;
        
        /**
         * constructor.
         * 
         * @since 1.0
         */
        public Builder() {
            isCommitted = false;
        }

        /**
         * sets whether to track (cache) text source IDs, default is not to track.
         *
         * @param trackTextSourceIds whether to track (cache) text source IDs
         *
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to create an instance
         * 
         * @since 1.0.1
         */
        public Builder setTrackTextSourceIds(final boolean trackTextSourceIds) {
            check();
            this.trackTextSourceIds = trackTextSourceIds;
            return this;
        }

        /**
         * sets whether to track (cache) file source last modified, default is not to track.
         *
         * @param trackFileSourceLastModified whether to track (cache) file source last modified
         *
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to create an instance
         * 
         * @since 1.0.1
         */
        public Builder setTrackFileSourceLastModified(final boolean trackFileSourceLastModified) {
            check();
            this.trackFileSourceLastModified = trackFileSourceLastModified;
            return this;
        }

        /**
         * sets latency for tracking file last modified of file sources,
         * defaults is {@link #DEFAULT_FILE_LAST_MODIFIED_TRACKING_LATENCY_MS}.
         * <p>
         * Only has an effect if also set to track file source last modified.
         *
         * @param fileLastModifiedTrackingLatencyMs latency for tracking file last modified of file sources
         *
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to create an instance
         * 
         * @since 1.0.1
         */
        public Builder setFileLastModifiedTrackingLatencyMs(final long fileLastModifiedTrackingLatencyMs) {
            check();
            this.fileLastModifiedTrackingLatencyMs = fileLastModifiedTrackingLatencyMs;
            return this;
        }

        /**
         * sets whether to track URL content, default is not to track,
         * {@literal i.e.} to consider URL content static.
         *
         * @param trackUrlContent whether to track URL content
         *
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to create an instance
         * 
         * @since 1.0
         */
        public Builder setTrackUrlContent(final boolean trackUrlContent) {
            check();
            this.trackUrlContent = trackUrlContent;
            return this;
        }

        /**
         * sets latency for tracking URL content, defaults is {@link #DEFAULT_URL_TRACKING_LATENCY_MS}.
         * <p>
         * Only has an effect if also set to track URL content.
         *
         * @param urlTrackingLatencyMs latency for tracking URL content
         *
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to create an instance
         * 
         * @since 1.0
         */
        public Builder setUrlTrackingLatencyMs(final long urlTrackingLatencyMs) {
            check();
            this.urlTrackingLatencyMs = urlTrackingLatencyMs;
            return this;
        }

        /**
         * gets whether to track (cache) text source IDs, default is not to track.
         *
         * @return whether to track (cache) text source IDs
         * 
         * @since 1.0.1
         */
        public boolean isTrackTextSourceIds() {
            return trackTextSourceIds;
        }

        /**
         * gets whether to track (cache) file source last modified, default is not to track.
         *
         * @return whether to track (cache) file source last modified
         * 
         * @since 1.0.1
         */
        public boolean isTrackFileSourceLastModified() {
            return trackFileSourceLastModified;
        }

        /**
         * gets latency for tracking file last modified of file sources.
         *
         * @return latency for tracking file last modified of file sources
         * 
         * @since 1.0.1
         */
        public long getFileLastModifiedTrackingLatencyMs() {
            return fileLastModifiedTrackingLatencyMs;
        }
        
        /**
         * gets whether to track URL content.
         *
         * @return whether to track URL content
         * 
         * @since 1.0
         */
        public boolean isTrackUrlContent() {
            return trackUrlContent;
        }

        /**
         * gets latency for tracking URL content.
         *
         * @return latency for tracking URL content
         * 
         * @since 1.0
         */
        public long getUrlTrackingLatencyMs() {
            return urlTrackingLatencyMs;
        }
        
        private Builder commit() {
            if (!isCommitted) {
                if (urlTrackingLatencyMs < 0) {
                    urlTrackingLatencyMs = DEFAULT_URL_TRACKING_LATENCY_MS;
                }
                if (fileLastModifiedTrackingLatencyMs < 0) {
                    fileLastModifiedTrackingLatencyMs = DEFAULT_FILE_LAST_MODIFIED_TRACKING_LATENCY_MS;
                }
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds an instance of {@link DefaultSourceFactory}.
         *
         * @return instance
         * 
         * @since 1.0
         */
        public DefaultSourceFactory build() {
            commit();
            return new DefaultSourceFactory(this);
        }
                
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

}
