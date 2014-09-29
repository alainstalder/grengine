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
    private final boolean trackUrlContent;
    private final long urlTrackingLatencyMs;
    private Map<Source,TrackingInfo> trackingMap;
    
    /**
     * constructor from builder.
     * 
     * @since 1.0
     */
    protected DefaultSourceFactory(final Builder builder) {
        this.builder = builder.commit();
        trackUrlContent = builder.isTrackUrlContent();
        urlTrackingLatencyMs = builder.getUrlTrackingLatencyMs();
        trackingMap = new ConcurrentHashMap<Source,TrackingInfo>();
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
        return new DefaultTextSource(text);
    }

    @Override
    public Source fromText(final String text, final String desiredClassName) {
        return new DefaultTextSource(text, desiredClassName);
    }

    @Override
    public Source fromFile(final File file) {
        return new DefaultFileSource(file);
    }

    @Override
    public Source fromUrl(final URL url) {
        if (trackUrlContent) {
            return new TrackingUrlSource(url);
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
        trackingMap.clear();
    }
    
    /**
     * gets the builder.
     * 
     * @since 1.0
     */
    public Builder getBuilder() {
        return builder;
    }
    
    class TrackingUrlSource extends DefaultUrlSource {
        
        private TrackingUrlSource(URL url) {
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

            TrackingInfo info = trackingMap.get(this);
            if (info != null) {
                // check both boundaries of the interval to exclude problems with leap seconds etc.
                long diff = System.currentTimeMillis() - info.lastChecked;
                if (diff >= 0 && diff < urlTrackingLatencyMs) {
                    return info.lastModified;
                }
            }

            synchronized(this) {
                // prevent multiple updates
                info = trackingMap.get(this);
                if (info != null) {
                    long diff = System.currentTimeMillis() - info.lastChecked;
                    if (diff >= 0 && diff < urlTrackingLatencyMs) {
                        return info.lastModified;
                    }
                }
                
                String textHashNew = getTextHash();
                long now = System.currentTimeMillis();
                if (info != null && info.textHash.equals(textHashNew)) {
                    trackingMap.put(this, new TrackingInfo(now, info.lastModified, info.textHash));
                    return info.lastModified;
                } else {
                    trackingMap.put(this, new TrackingInfo(now, now, textHashNew));
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
        
        private boolean isCommitted;
        
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
         * sets whether to track URL content, default is not to track,
         * {@literal i.e.} to consider URL content static.
         * 
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
         * 
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
         * gets whether to track URL content.
         * 
         * @since 1.0
         */
        public boolean isTrackUrlContent() {
            return trackUrlContent;
        }

        /**
         * gets latency for tracking URL content.
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
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds an instance of {@link DefaultSourceFactory}.
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
