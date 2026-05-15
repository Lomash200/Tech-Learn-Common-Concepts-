package com.techlearner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * JVM Internals Service
 * ═══════════════════════════════════════════════════════════════
 *
 * TOPIC: JVM Memory Model
 * TOPIC: Garbage Collection
 * TOPIC: HashMap Internal
 * TOPIC: ConcurrentHashMap
 * TOPIC: Java Streams Internals
 */
@Service
@Slf4j
public class JvmInternalsService {

    /**
     * ═══════════════════════════════════════════════════════════
     * TOPIC: JVM Memory Model
     * ═══════════════════════════════════════════════════════════
     *
     * JVM Memory Areas:
     * ─────────────────────────────────────
     * 1. HEAP:
     *    - Young Generation:
     *      * Eden Space (new objects yahan bante hain)
     *      * Survivor 0 (S0)
     *      * Survivor 1 (S1)
     *    - Old Generation (Tenured):
     *      * Long-lived objects
     *
     * 2. NON-HEAP (Metaspace):
     *    - Class definitions, static variables, method bytecode
     *    - JDK 8 mein PermGen → Metaspace (native memory, auto-grows)
     *
     * 3. Stack:
     *    - Per-thread! Each thread ka apna stack
     *    - Local variables, method calls, return addresses
     *    - Stack overflow → StackOverflowError
     *
     * 4. PC Register (Program Counter):
     *    - Per-thread, current instruction track karta hai
     *
     * 5. Native Method Stack:
     *    - JNI (Java Native Interface) ke liye
     *
     * JVM Flags for tuning:
     * -Xms512m  → Initial heap size
     * -Xmx2g    → Maximum heap size
     * -Xss256k  → Thread stack size
     * -XX:MetaspaceSize=256m → Metaspace initial size
     */
    public Map<String, Object> getJvmMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();

        Map<String, Object> info = new LinkedHashMap<>();

        // Heap Memory
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        Map<String, String> heap = new LinkedHashMap<>();
        heap.put("used", formatBytes(heapUsage.getUsed()));
        heap.put("committed", formatBytes(heapUsage.getCommitted()));
        heap.put("max", formatBytes(heapUsage.getMax()));
        heap.put("usedPercent", String.format("%.1f%%",
                (double) heapUsage.getUsed() / heapUsage.getMax() * 100));
        info.put("heap", heap);

        // Non-Heap (Metaspace)
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        Map<String, String> nonHeap = new LinkedHashMap<>();
        nonHeap.put("used", formatBytes(nonHeapUsage.getUsed()));
        nonHeap.put("committed", formatBytes(nonHeapUsage.getCommitted()));
        info.put("nonHeap_metaspace", nonHeap);

        // Memory Pools (Eden, Survivor, Old Gen etc.)
        List<Map<String, String>> pools = memoryPools.stream()
                .map(pool -> {
                    Map<String, String> poolInfo = new LinkedHashMap<>();
                    poolInfo.put("name", pool.getName());
                    poolInfo.put("type", pool.getType().toString());
                    if (pool.getUsage() != null) {
                        poolInfo.put("used", formatBytes(pool.getUsage().getUsed()));
                        poolInfo.put("max", pool.getUsage().getMax() > 0
                                ? formatBytes(pool.getUsage().getMax()) : "unlimited");
                    }
                    return poolInfo;
                })
                .collect(Collectors.toList());
        info.put("memoryPools", pools);

        // Runtime info
        Map<String, Object> runtimeInfo = new LinkedHashMap<>();
        runtimeInfo.put("availableProcessors", runtime.availableProcessors());
        runtimeInfo.put("totalMemory", formatBytes(runtime.totalMemory()));
        runtimeInfo.put("freeMemory", formatBytes(runtime.freeMemory()));
        runtimeInfo.put("maxMemory", formatBytes(runtime.maxMemory()));
        info.put("runtime", runtimeInfo);

        return info;
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * TOPIC: Garbage Collection
     * ═══════════════════════════════════════════════════════════
     *
     * GC Types:
     * ─────────────────────────────────────
     * 1. Minor GC (Young GC):
     *    - Eden space full → GC
     *    - Surviving objects → S0/S1
     *    - After N survivals → Old Gen (promotion)
     *    - Fast, frequent
     *
     * 2. Major GC (Old Gen GC):
     *    - Old Gen full → GC
     *    - Slower, less frequent
     *    - Stop-The-World pauses (STW)
     *
     * 3. Full GC:
     *    - Entire heap + Metaspace
     *    - Avoid! Causes app pauses
     *    - Triggered by: System.gc(), Metaspace full, promotion failure
     *
     * GC Algorithms:
     * ─────────────────────────────────────
     * Serial GC    → Single thread, small heaps
     * Parallel GC  → Multiple threads, throughput focused (default Java 8)
     * G1 GC        → Region-based, balanced (default Java 9+)
     * ZGC          → Ultra-low latency (<1ms pauses), Java 15+
     * Shenandoah  → Low pause, concurrent compaction
     *
     * GC Roots (what GC doesn't collect):
     * - Local variables in active threads
     * - Static variables
     * - JNI references
     *
     * JVM Flags:
     * -XX:+UseG1GC → Use G1
     * -XX:+UseZGC  → Use ZGC
     * -XX:+PrintGCDetails → GC logs
     * -Xlog:gc    → GC logging (Java 11+)
     */
    public Map<String, Object> getGcStats() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        Map<String, Object> gcInfo = new LinkedHashMap<>();
        gcInfo.put("gcAlgorithmExplanation", Map.of(
                "current_default", "G1GC (Java 11+)",
                "minor_gc", "Young generation (Eden → Survivor)",
                "major_gc", "Old generation collection",
                "full_gc", "Complete heap + Metaspace (avoid!)"
        ));

        List<Map<String, Object>> gcDetails = gcBeans.stream()
                .map(gc -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("name", gc.getName());
                    detail.put("collectionCount", gc.getCollectionCount());
                    detail.put("collectionTimeMs", gc.getCollectionTime());
                    detail.put("memoryPoolNames", Arrays.asList(gc.getMemoryPoolNames()));
                    return detail;
                })
                .collect(Collectors.toList());

        gcInfo.put("gcCollectors", gcDetails);

        // Thread count (TOPIC: Threads Internals)
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threadInfo = new LinkedHashMap<>();
        threadInfo.put("liveThreadCount", threadMXBean.getThreadCount());
        threadInfo.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        threadInfo.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        threadInfo.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());
        gcInfo.put("threads", threadInfo);

        return gcInfo;
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * TOPIC: HashMap Internal
     * ═══════════════════════════════════════════════════════════
     *
     * HashMap kaise kaam karta hai:
     * ─────────────────────────────────────
     * 1. Array of buckets (Node<K,V>[] table)
     * 2. key.hashCode() → hash → index = hash & (capacity-1)
     * 3. Same index? → Collision! → Linked List in that bucket
     * 4. Java 8+: List → Tree (Red-Black Tree) when bucket size > 8
     *    Tree conversion: O(n) list operations → O(log n) tree operations
     *
     * Hash Function:
     * static final int hash(Object key) {
     *     int h = key.hashCode();
     *     return (h) ^ (h >>> 16);  // XOR upper bits with lower bits
     * }
     * Purpose: Better distribution, reduces collisions
     *
     * Resize (Rehashing):
     * When load factor > 0.75 (default) → capacity double karo
     * All entries rehash → expensive! O(n)
     * Avoid: new HashMap<>(initialCapacity)
     *
     * NOT thread-safe! Race condition in resize → infinite loop possible (Java 7)
     * Use: ConcurrentHashMap for concurrent access
     *
     * Time Complexity:
     * Best case: O(1) (no collision)
     * Worst case: O(n) (all keys same hash - DoS attack possible!)
     * Average: O(1)
     * With tree bins: O(log n)
     */
    public Map<String, Object> demonstrateHashMapInternals() {
        Map<String, Object> demo = new LinkedHashMap<>();

        // HashMap creation with initial capacity (avoids early resize)
        HashMap<String, Integer> map = new HashMap<>(32, 0.75f);
        // 32 = initial capacity, 0.75 = load factor
        // Resize when: size > 32 * 0.75 = 24 entries

        // Demonstrating hash collisions (same bucket)
        // "FB" and "Ea" have same hashCode in Java!
        demo.put("collision_example", Map.of(
                "note", "'FB'.hashCode() == 'Ea'.hashCode() in Java",
                "hashFB", "FB".hashCode(),
                "hashEa", "Ea".hashCode(),
                "explanation", "Same bucket → Linked list → O(n) lookup instead of O(1)"
        ));

        demo.put("internal_structure", Map.of(
                "default_initial_capacity", 16,
                "default_load_factor", 0.75,
                "resize_threshold", "capacity * load_factor",
                "treeify_threshold", 8,
                "untreeify_threshold", 6
        ));

        demo.put("time_complexity", Map.of(
                "get_best", "O(1)",
                "get_worst", "O(n) [all collisions]",
                "get_tree_bucket", "O(log n)",
                "put", "O(1) amortized [O(n) on resize]"
        ));

        return demo;
    }

    /**
     * ═══════════════════════════════════════════════════════════
     * TOPIC: ConcurrentHashMap
     * ═══════════════════════════════════════════════════════════
     *
     * HashMap vs ConcurrentHashMap vs Hashtable:
     * ─────────────────────────────────────
     * HashMap: Not thread-safe, fastest single-thread
     * Hashtable: Synchronized (entire map lock), thread-safe but SLOW
     * ConcurrentHashMap: Segment/bucket-level locking, high concurrency
     *
     * Java 8 ConcurrentHashMap Internals:
     * ─────────────────────────────────────
     * 1. Bucket-level locking (NOT segment locking like Java 7)
     * 2. CAS (Compare-And-Swap) operations for atomic updates
     * 3. Volatile reads for visibility across threads
     * 4. Synchronized only on individual bucket nodes (not whole map)
     *
     * Concurrency Level:
     * Default = 16 → 16 different threads can write simultaneously
     * (one thread per bucket, max 16 concurrent writes without contention)
     *
     * Important: null keys/values NOT allowed (unlike HashMap)
     * Reason: Can't distinguish "key not found" vs "key maps to null"
     *         in concurrent scenarios
     *
     * Atomic Operations:
     * putIfAbsent(k, v)   → If absent, put (atomic)
     * computeIfAbsent(k, f) → Compute and put if absent (atomic)
     * merge(k, v, f)        → Merge with existing (atomic)
     */
    public Map<String, Object> demonstrateConcurrentHashMap() {
        ConcurrentHashMap<String, Integer> concMap = new ConcurrentHashMap<>();

        // Thread-safe atomic operations
        concMap.put("apples", 10);
        concMap.put("bananas", 5);

        // Atomic increment (no race condition!)
        concMap.merge("apples", 1, Integer::sum);  // apples = 10 + 1 = 11

        // Atomic compute
        concMap.computeIfAbsent("oranges", k -> 0);

        // putIfAbsent - atomic check-and-set
        Integer existing = concMap.putIfAbsent("apples", 99);  // Returns old value (11), doesn't update

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mapContents", concMap);
        result.put("concepts", Map.of(
                "segmentLocking_java7", "16 segments, each has own ReentrantLock",
                "bucketLocking_java8", "Synchronized on individual bin nodes",
                "casOperations", "Compare-And-Swap for lock-free updates",
                "volatileReads", "Ensures visibility across threads",
                "nullNotAllowed", "null keys/values cause NullPointerException"
        ));

        return result;
    }

    /**
     * TOPIC: Threads Internals - Thread Pool info
     */
    public Map<String, Object> getThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        Map<String, Object> info = new LinkedHashMap<>();

        // All thread IDs
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);

        // Group by state
        Map<String, Long> stateCount = Arrays.stream(threadInfos)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        ti -> ti.getThreadState().name(),
                        Collectors.counting()
                ));

        info.put("threadStateDistribution", stateCount);
        info.put("totalThreads", threadIds.length);

        // Thread states explanation
        info.put("threadStates", Map.of(
                "NEW", "Created but not started",
                "RUNNABLE", "Running or ready to run",
                "BLOCKED", "Waiting to acquire a monitor lock",
                "WAITING", "Waiting indefinitely (wait(), join())",
                "TIMED_WAITING", "Waiting with timeout (sleep(), join(timeout))",
                "TERMINATED", "Finished execution"
        ));

        // Deadlock detection!
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        info.put("deadlockedThreads",
                deadlockedThreads != null ? deadlockedThreads.length : 0);

        // Sample some thread names
        List<String> sampleThreads = Arrays.stream(threadInfos)
                .filter(Objects::nonNull)
                .limit(15)
                .map(ti -> ti.getThreadName() + " [" + ti.getThreadState() + "]")
                .collect(Collectors.toList());
        info.put("sampleThreads", sampleThreads);

        return info;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
