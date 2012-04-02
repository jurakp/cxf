/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.security.tokenstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.util.StringUtils;

/**
 * A simple HashMap-based TokenStore. The default TTL is 5 minutes and the max TTL is 1 hour.
 */
public class MemoryTokenStore implements TokenStore {
    public static final long DEFAULT_TTL = 60L * 5L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;
    
    private Map<String, CacheEntry> tokens = new ConcurrentHashMap<String, CacheEntry>();
    
    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            CacheEntry cacheEntry = null;
            if (token.getExpires() == null) {
                Date expires = new Date();
                long currentTime = expires.getTime();
                expires.setTime(currentTime + (DEFAULT_TTL * 1000L));
                cacheEntry = new CacheEntry(token, expires);
            } else {
                Date expires = token.getExpires();
                Date current = new Date();
                long expiryTime = expires.getTime() - current.getTime();
                if (expiryTime < 0 || expiryTime > (MAX_TTL * 1000L)) {
                    expires.setTime(current.getTime() + (DEFAULT_TTL * 1000L));
                }
                cacheEntry = new CacheEntry(token, expires);
            }
            
            tokens.put(token.getId(), cacheEntry);
        }
    }
    
    public void remove(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            tokens.remove(token.getId());
        }
    }

    public Collection<String> getTokenIdentifiers() {
        processTokenExpiry();
        return tokens.keySet();
    }
    
    public Collection<SecurityToken> getExpiredTokens() {
        List<SecurityToken> expiredTokens = new ArrayList<SecurityToken>();
        Date current = new Date();
        synchronized (tokens) {
            for (String id : tokens.keySet()) {
                CacheEntry cacheEntry = tokens.get(id);
                if (cacheEntry.getExpiry().before(current)) {
                    expiredTokens.add(cacheEntry.getSecurityToken());
                }
            }
        }
        return expiredTokens;
    }
    
    public SecurityToken getToken(String id) {
        processTokenExpiry();
        
        CacheEntry cacheEntry = tokens.get(id);
        if (cacheEntry != null) {
            return cacheEntry.getSecurityToken();
        }
        return null;
    }
    
    public SecurityToken getTokenByAssociatedHash(int hashCode) {
        processTokenExpiry();
        
        synchronized (tokens) {
            for (String id : tokens.keySet()) {
                CacheEntry cacheEntry = tokens.get(id);
                SecurityToken securityToken = cacheEntry.getSecurityToken();
                if (hashCode == securityToken.getAssociatedHash()) {
                    return securityToken;
                }
            }
        }
        return null;
    }

    protected void processTokenExpiry() {
        Date current = new Date();
        synchronized (tokens) {
            for (String id : tokens.keySet()) {
                CacheEntry cacheEntry = tokens.get(id);
                if (cacheEntry.getExpiry().before(current)) {
                    tokens.remove(id);
                }
            }
        }
    }
    
    private static class CacheEntry {
        
        private final SecurityToken securityToken;
        private final Date expires;
        
        public CacheEntry(SecurityToken securityToken, Date expires) {
            this.securityToken = securityToken;
            this.expires = expires;
        }
        
        /**
         * Get the SecurityToken
         * @return the SecurityToken
         */
        public SecurityToken getSecurityToken() {
            return securityToken;
        }
        
        /**
         * Get when this CacheEntry is to be removed from the cache
         * @return when this CacheEntry is to be removed from the cache
         */
        public Date getExpiry() {
            return expires;
        }
        
    }
 
}
