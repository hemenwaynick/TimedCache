/*
 * Implementation of cache that can be used to cache results of arbitrary delegate function.
 */

import java.util.function.Function;
import java.util.*;

public class TimedCache<T,R> implements Function<T,R> {

    protected final Function<T,R> delegate;
    private HashMap<T,CacheValue> cache;

    protected class CacheValue {
        public long timeOfEntry = System.currentTimeMillis();
        public R val;

        protected CacheValue(R val) {
            this.val = val;
        }
    }

    protected TimedCache(Function<T,R> delegate, long timeLimit) {
        this.delegate = Objects.requireNonNull(delegate);
        this.cache = new HashMap<T,CacheValue>();

        new Thread(() -> {
            while (true) {
                updateCache(timeLimit);
            }
        }).start();
    }

    public R apply(T t) {
        R res;

        if (this.cache.containsKey(t)) {
            res = this.cache.get(t).val;
        } else {
            res = this.delegate.apply(t);
            this.cache.put(t, new CacheValue(res));
        }

        return res;
    }

    protected void updateCache(long timeLimit) {
        for (Map.Entry entry : this.cache.entrySet()) {
            long currentTime = System.currentTimeMillis();

            if ((currentTime - ((CacheValue) entry.getValue()).timeOfEntry) >= timeLimit) {
                this.cache.remove(entry.getKey());
            }
        }
    }

    public static void main(String[] args) {
        TimedCache<String,String> tc = new TimedCache<String,String>(x -> x.trim(), 1000);

        tc.apply("  test  ");

        System.out.println("Does 'apply' method store value in cache: " + tc.cache.containsKey("  test  "));
        System.out.println("Value stored in cache by 'apply' method: " + (tc.cache.get("  test  ")).val);

        new Thread(() -> {
            try {
            Thread.sleep(3000);
            System.out.println("Does 'updateCache' method remove entry from cache after time has passed: " + !tc.cache.containsKey("  test  "));
            } catch (InterruptedException ex) {}
        }).start();
    }

}