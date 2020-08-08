package com.nikondsl.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
	// write your code here
        CacheProvider<String, SimpleFuture<String, Integer, Exception>> cacheProvider = new CacheProvider<String, SimpleFuture<String, Integer, Exception>>() {
            private ConcurrentMap<String, SimpleFuture<String, Integer, Exception> > cache = new ConcurrentHashMap<>();
    
            @Override
            public String getName() {
                return "default";
            }
    
            @Override
            public SimpleFuture<String, Integer, Exception>  get(String key) {
                return cache.get(key);
            }
    
            @Override
            public SimpleFuture<String, Integer, Exception>  putIfAbsent(String key, SimpleFuture<String, Integer, Exception>  value) {
                return cache.putIfAbsent(key, value);
            }
    
            @Override
            public SimpleFuture<String, Integer, Exception>  remove(String key) {
                return cache.remove(key);
            }
    
            @Override
            public Iterable<Map.Entry<String, SimpleFuture<String, Integer, Exception> >> getEntries() {
                return cache.entrySet();
            }
        };
        ValueProvider<String, Integer, Exception> valueProvider = new ValueProvider<String, Integer, Exception>() {
            @Override
            public Integer createValue(String key) throws Exception {
                TimeUnit.SECONDS.sleep(3);
                return key.length();
            }
    
            @Override
            public long getTimeToLive() {
                return 5_000L;
            }
        };
        
        SingleCalculationLatch<String, Integer, Exception> latch = new SingleCalculationLatch(cacheProvider, valueProvider);
    
        ExecutorService service = Executors.newFixedThreadPool(20);
        long time =System.currentTimeMillis();
        service.submit(()-> {
            try {
                System.out.println("abc="+latch.get("abc"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("abcde="+latch.get("abcde"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("abcde="+latch.get("abcde"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("ab="+latch.get("ab"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("abc="+latch.get("abc"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("abc="+latch.get("abc"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("abc="+latch.get("abc"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("abc="+latch.get("abc"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("ab="+latch.get("ab"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        service.submit(()-> {
            try {
                System.out.println("abcde="+latch.get("abcde"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        
        service.shutdown();
        if(service.awaitTermination(1, TimeUnit.MINUTES)){
            System.out.println("OK, time "+(System.currentTimeMillis()-time)+" ms");
        }
        
        latch.stop();
    }
    
    
}
