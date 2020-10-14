package com.nikondsl;

import com.nikondsl.cache.CacheProvider;
import com.nikondsl.cache.ReferenceType;
import com.nikondsl.cache.SimpleFuture;
import com.nikondsl.cache.SingleCalculationLatch;
import com.nikondsl.cache.ValueProvider;
import org.ehcache.Cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MainNullValues {
    
    static class Holder {
       private byte[] bytes = new byte[10240];
       private int number;
    
        public Holder(int length) {
            number = length;
        }
    }
    
    static CacheProvider<String, SimpleFuture<String, Holder, Exception>> cacheProvider = new CacheProvider<String, SimpleFuture<String, Holder, Exception>>() {
        private ConcurrentMap<String, SimpleFuture<String, Holder, Exception> > cache = new ConcurrentHashMap<>();
        
        @Override
        public String getName() {
            return "default";
        }
        
        @Override
        public SimpleFuture<String, Holder, Exception>  get(String key) {
            return cache.get(key);
        }
        
        @Override
        public SimpleFuture<String, Holder, Exception>  putIfAbsent(String key, SimpleFuture<String, Holder, Exception>  value) {
            
            SimpleFuture<String, Holder, Exception> future = cache.putIfAbsent(key, value);
            return future;
        }
        
        @Override
        public SimpleFuture<String, Holder, Exception>  remove(String key) {
            return cache.remove(key);
        }
    
        @Override
        public void forEach(Consumer<Cache.Entry<String, SimpleFuture<String, Holder, Exception>>> consumer) {
            cache.forEach((key, value) -> consumer.accept(new Cache.Entry<String, SimpleFuture<String, Holder, Exception>>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public SimpleFuture<String, Holder, Exception> getValue() {
                    return value;
                }
            }));
        }
    };
    
    static ValueProvider<String, Holder, Exception> valueProvider = new ValueProvider<String, Holder, Exception>() {
        @Override
        public Holder createValue(String key) throws Exception {
            return null;
        }
        
        @Override
        public long getTimeToLive() {
            return 1_000L;
        }
        
        @Override
        public ReferenceType getReferenceType() {
            return ReferenceType.SOFT;
        }
    };
    
    static SingleCalculationLatch<String, Holder, Exception> latch = new SingleCalculationLatch(cacheProvider, valueProvider);
    
    
    public static void main(String[] args) throws Exception {
        latch.setSleepBeforeDelete(5_000);
        System.out.println(latch.get("12345"));
        System.out.println(latch.get("12345"));
        System.out.println(latch.get("12345"));
        System.out.println(latch.get("12345"));
        
        
        latch.stop();
    }
}
