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

public class Main {
    
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
            TimeUnit.MILLISECONDS.sleep(750);
            return new Holder(key.length());
        }
        
        @Override
        public long getTimeToLive() {
            return 300_000L;
        }
        
        @Override
        public ReferenceType getReferenceType() {
            return ReferenceType.SOFT;
        }
    };
    
    static SingleCalculationLatch<String, Holder, Exception> latch = new SingleCalculationLatch(cacheProvider, valueProvider);
    
    static ExecutorService service = Executors.newFixedThreadPool(20);
    
    public static void main(String[] args) throws Exception {
        latch.setSleepBeforeDelete(5_000);
        long time = System.currentTimeMillis();
        for(int i=0;i<1000_000;i++) {
            if (i % 10_000 == 0) {
                System.err.println("sleep...");
                TimeUnit.MILLISECONDS.sleep(3000);
            }
            if (i % 10_000 == 0) System.out.println("#"+i+". completed: "+((ThreadPoolExecutor) service).getCompletedTaskCount());
            main1(args);
        }
    
        service.shutdown();
        while(!service.awaitTermination(10, TimeUnit.SECONDS)){
            System.out.println("Waiting... time "+(System.currentTimeMillis()-time)+" ms, left: "+(
                    (ThreadPoolExecutor) service).getActiveCount() +", completed: "+ ((ThreadPoolExecutor) service).getCompletedTaskCount());
        }
        latch.stop();
    }
    
    private static String letters = "abcdefghijklmnopqrstuvxyzklmnopqrstu1234567890!@#$%";
    static SyncRandom random = new SyncRandom();
    
    private static String generateRandomText(int length) {
        StringBuilder result = new StringBuilder(16);
        int size = letters.length();
        for (int i = 0; i < length; i++) {
            result.append(letters.charAt((int) (random.nextDouble() * size)));
        }
        return result.toString();
    }
    
    public static void main1(String[] args) throws Exception {
	    // write your code here
        
        service.execute(()-> {
            try {
                String key = generateRandomText(2);
                latch.get(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
