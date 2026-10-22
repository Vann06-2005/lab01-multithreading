# Lab-01: Multithreading in Java

Software Engineering · Chapter 01 · JDK 25 · Maven

```bash
mvn -q verify                                   # build + JUnit tests
java -cp target/classes edu.itc.se.lab01.task1.Main
```

All timings below were measured on one machine: 20 logical cores, OpenJDK 25.0.4.

## Task 1 · Creating and running threads

Four techniques: a `Thread` subclass (`apollo`), a `Runnable` (`gemini`), a lambda
(`mercury`) and the virtual-thread builder (`artemis`).

```
Mission control: all launched
...
Mission control: all landed
apollo   virtual=false state=TERMINATED
gemini   virtual=false state=TERMINATED
mercury  virtual=false state=TERMINATED
artemis  virtual=true  state=TERMINATED
```

**`start()` vs `run()`.** `start()` creates a new thread that runs `run()`, so `main`
continues right away. "all launched" prints first and the countdowns interleave.
`run()` is a normal method call on the main thread. Each countdown finishes before the
next one starts, "all launched" prints last, and the `Runnable`-based countdowns print
`main: 3 ...` because `Thread.currentThread()` is `main`. No thread is ever started, so
`getState()` stays `NEW` with `--sequential`.

## Task 2 · Fixing a race condition

8 threads × 1 000 000 increments, expected 8 000 000 (typical run):

| Counter | Value | Correct | Time (ms) |
|---|---|---|---|
| UnsafeCounter | 2,843,080 | false | 45 |
| SynchronizedCounter | 8,000,000 | true | 1485 |
| LockCounter | 8,000,000 | true | 218 |
| AtomicCounter | 8,000,000 | true | 123 |
| LongAdderCounter | 8,000,000 | true | 16 |

UnsafeCounter gave 2,843,080 / 3,088,440 / 2,507,566 over three runs: about 60–70 % of
the increments were lost. `count++` is read → add → write, so two threads can read the
same value and both write value + 1.

- **synchronized** is the slowest here. Eight threads fight over one monitor, so it
  inflates into an OS-level lock and the threads park and unpark constantly.
- **ReentrantLock** spins briefly before parking, which works better for a critical
  section this short.
- **AtomicLong** has no lock (a CAS loop), but every thread still hits the same memory
  word, and failed CAS attempts make it bounce between cores.
- **LongAdder** spreads the increments over several cells, so threads rarely collide.
  `sum()` adds the cells up. It is the fastest under write contention.

**Why synchronizing only `increment()` is still a bug.** `synchronized` gives both mutual
exclusion and visibility (a happens-before edge between unlock and the next lock of the
same monitor). An unsynchronized `get()` has no such edge with the writers, so it can
return a stale value from a CPU cache or register. Because `count` is a `long`, the JLS
also allows a torn read: half of the old value and half of the new. The reader must
take the same lock (or the field must be `volatile`/atomic).

## Task 3 · Producer–consumer

2 producers × 50 orders, 3 consumers, capacity 5:

```
Total processed: 100 (expected 100) in 632 ms      # BoundedBuffer (Main)
Total processed: 100 (expected 100) in 648 ms      # ArrayBlockingQueue (Main2)
```

`Producer` and `Consumer` depend only on the `OrderQueue` interface. `Main2` plugs
in `BlockingOrderQueue` (which wraps `ArrayBlockingQueue`) without changing them.
Run with `java -ea` to enable the capacity assertions in `BoundedBuffer`.

The lab has each producer put the poison pills. With two producers, the first one to
finish could then stop all three consumers while the other still has orders, and that
producer would block forever on a full buffer. So only the **last** producer to finish
puts the pills (tracked with an `AtomicInteger`).

**What breaks with `notify()` + `if`.** `BrokenBoundedBuffer` is that variant
(`task3.Main --broken`). I ran it 20 times, and **it hung all 20 times**. Each run first
threw `java.util.NoSuchElementException` in a consumer (after 18–60 orders), then
deadlocked.

- **`if` instead of `while`:** a woken thread does not recheck the condition. A consumer
  can wake up after another consumer has already emptied the buffer (or from a spurious
  wake-up), and `removeFirst()` throws. The same thing happens to producers: they can
  push past the capacity.
- **`notify()` instead of `notifyAll()`:** one wait set holds both producers and
  consumers, and `notify()` wakes just one arbitrary thread. A consumer that takes an
  item can wake another consumer instead of a waiting producer, and that signal is
  lost. Once the consumers have died or are waiting, the producers block on a full
  buffer forever.

`while` + `notifyAll()` is correct because every woken thread rechecks its own
condition, and the thread that can actually make progress is always among those woken.

## Task 4 · Thread pools, Future and virtual threads

```
single-threaded: 283146 primes in 1112 ms
pool (20 threads): 283146 primes in 148 ms
downloads on fixed(20) platform threads: ok=489 failed=10 timedOut=1 in 5216 ms
downloads on virtual threads           : ok=489 failed=10 timedOut=1 in 1209 ms
CompletableFuture on virtual threads   : ok=489 failed=10 timedOut=1 in 1009 ms
```

- **CPU-bound:** 16 chunks on a pool sized to the core count give the same 283 146
  primes about 7.5× faster. The chunks are not equally expensive (higher numbers need
  more trial divisions), so the speed-up is less than 20×.
- **I/O-bound:** with 20 platform threads, only 20 of the 500 tasks can sleep at the
  same time. 500 / 20 × 200 ms = 25 rounds ≈ 5 s. A virtual thread that blocks in
  `sleep` (or real I/O) gives up its carrier thread. All 500 tasks wait in parallel on
  a handful of carrier threads, so the run takes about as long as the slowest task,
  which here is the 1 s timeout. Virtual threads are cheap (a few hundred bytes of
  stack on the heap), so one per task is fine. Platform threads are OS threads with
  MB-sized stacks, and you cannot have 500 000 of them.
- **Failures:** every 50th download throws `IOException("boom")`. `future.get()`
  wraps it in an `ExecutionException`, which is caught and counted (10 failed).
- **Timeout:** download #7 sleeps 5 s. `get(1, SECONDS)` throws `TimeoutException`,
  and `cancel(true)` interrupts that sleep.
- **Shutdown:** every executor is closed with try-with-resources. The JVM exits on its
  own without `System.exit`.
- **Bonus:** `CompletableFuture.supplyAsync(..., executor).orTimeout(1, SECONDS)` with
  `handle()` turns failures and timeouts into fallback strings, and `allOf()` waits for
  all of them. Note that `orTimeout` does not interrupt the task, so closing that
  executor still waits for the 5 s download.

## Task 5 · Deadlock and graceful shutdown

- **Deadlock:** `Bank` hangs within a second. The jstack report is in
  [`deadlock.txt`](deadlock.txt): `transfer-1` and `transfer-5` each hold the account the
  other one wants, and `transfer-0`/`transfer-7` are stuck behind them. The watchdog
  daemon (`findDeadlockedThreads()` every second) printed
  `[watchdog] DEADLOCK detected between: transfer-0, transfer-1, transfer-5, transfer-7`.
- **Fix A, `OrderedBank`:** both monitors are always locked in ascending account id.
  A cycle would need some thread to hold a higher id while waiting for a lower one,
  which can no longer happen.
  `transfers=800000 total=4000 (expected 4000) in 147 ms`
- **Fix B, `TryLockBank`:** `tryLock(10, MILLISECONDS)` on both locks. On failure the
  thread releases what it holds, backs off for a random 0.1–2 ms, and retries.
  `transfers=800000 retries=9200 total=4000 (expected 4000) in 22313 ms`.
  It never hangs, but it is much slower than Fix A. With 8 threads and only 4
  accounts, two threads often lock opposite accounts at the same time, and each such
  conflict costs a full 10 ms timeout before anyone backs off. Lock ordering avoids
  the conflict instead of detecting it, so it is the better fix whenever a global
  order exists.
- **Graceful shutdown** (in `TryLockBank`): workers loop
  `while (!Thread.currentThread().isInterrupted())`. After 2 s main interrupts them and
  joins with a shared 1 s deadline. `tryLock` and `sleep` throw
  `InterruptedException`, and the worker restores the flag with
  `Thread.currentThread().interrupt()` before leaving the loop.

  ```
  [main] interrupting 8 workers after 2000 ms
  transfer-0 stopped after 1813 transfers
  ...
  all workers stopped=true total=4000 (expected 4000)
  ```

## Challenge · Parallel word-frequency analyser

```bash
java -cp target/classes edu.itc.se.lab01.challenge.DataGenerator ./data      # 200 files × 50 000 words
java -cp target/classes edu.itc.se.lab01.challenge.WordCount ./data --top 5 --timeout 30
java -cp target/classes edu.itc.se.lab01.challenge.WordCount ./data --top 3 --timeout 1 --workers 1
```

```
[0.5s] files 116/153  words 3,025,853  5,934,510 w/s  top: the(505,721) and(262,516) of(186,694)
DONE  files=200 words=10,000,000 elapsed=0.80s mode=platform(20)
 rank  word              count     share
    1  the           1,222,277   12.22 %
    2  and             611,108    6.11 %
    3  of              407,538    4.08 %
    4  to              305,687    3.06 %
    5  a               244,673    2.45 %
result.csv written
```

How it works:
- **Scanner:** one thread walks the directory and feeds an `ArrayBlockingQueue<Path>(16)`.
  `put()` blocks when 16 paths are waiting (back-pressure), then the scanner adds one
  poison pill per worker.
- **Workers:** a fixed pool, or virtual threads with `--virtual`. They count into a
  `ConcurrentHashMap<String, LongAdder>`, and a `CountDownLatch` tells `run()` when all
  of them have finished.
- **Reporter:** a scheduled thread prints progress every 500 ms. It reads `LongAdder`
  sums and iterates the map without locking it (iteration is weakly consistent).
- **Stopping:** Ctrl+C (shutdown hook) and `--timeout` both call `shutdown()`, guarded
  by an `AtomicBoolean`. It interrupts the scanner and the workers, waits at most 2 s,
  and prints `[PARTIAL]`. Measured: `--timeout 1` printed `[PARTIAL]` at 1.02 s, and
  Ctrl+C stopped immediately with no stray JVM.
- **Tests (`WordCountTest`):**
  - Parallel results, for both platform and virtual workers, equal
    `ReferenceWordCount` on a 20-file fixture.
  - `--timeout 1` on 5 000 files stops in under 3 s with a partial result.
- `--workers N` is an extra option, added for the scaling table below.

**Scaling** (200 files, 10 M words, median of 3 runs):

| Workers | Elapsed (s) |
|---|---|
| 1 platform | 1.03 |
| 2 platform | 0.74 |
| 4 platform | 0.46 |
| 8 platform | 0.39 |
| 20 platform | 0.54 |
| 20 virtual | 0.61 |

Speed-up flattens after 4 workers and gets worse at 20. The data is only 58 MB, so
start-up and JIT warm-up are a large share of the time. Word frequencies are
Zipf-shaped (`the` is 12 % of all words), so many threads update the same few hot
entries and their cache lines bounce between cores. The work is CPU-bound and the
files sit in the page cache, so virtual threads give no advantage over platform
threads here.

**Why LongAdder / `merge` beats `synchronized` on the map.**
- A `synchronized` map serializes every update: only one of N workers makes progress
  at a time, so adding workers adds only waiting.
- `ConcurrentHashMap` locks per bin and only for structural changes. After the first
  time a word is seen, `computeIfAbsent` is a lock-free read that returns the existing
  `LongAdder`.
- `LongAdder.increment()` writes to a cell picked per thread, so even the hot word
  `the` takes concurrent increments without a shared lock. `merge(w, 1L, Long::sum)`
  also locks only that word's bin, never the whole map.

**Coffman conditions.** A deadlock needs all four of mutual exclusion, hold-and-wait,
no preemption and circular wait, so ruling out any one is enough. This design rules
out **hold-and-wait** and **circular wait**:
- Each worker holds at most one resource at a time. While it waits on `queue.take()`
  it holds no map bin, and while it updates a bin it waits for nothing else.
- `ConcurrentHashMap` locks one bin at a time, so no thread ever waits for a second
  bin while holding one.
- The only blocking points are the queue's internal lock and the latch, and nobody
  waits on them while holding another lock, so no cycle can form.

Cancellation through interruption also gives a form of preemption: `shutdown()` can
force a blocked scanner or worker out of `put()`/`take()`.
