# Lab-01: Multithreading in Java

Software Engineering · Chapter 01 · JDK 25 · Maven

Build and test: `mvn -q verify`

Run a task: `mvn -q compile && java -cp target/classes edu.itc.se.lab01.task1.Main`

## Task 1 · Creating and running threads

- `start()` vs `run()` (see the comment at the top of `task1/Main.java`):

## Task 2 · Fixing a race condition

| Counter | Value | Correct | Time (ms) |
|---|---|---|---|
| UnsafeCounter | | | |
| SynchronizedCounter | | | |
| LockCounter | | | |
| AtomicCounter | | | |
| LongAdderCounter | | | |

- Why is synchronizing only `increment()` (not `get()`) still a bug?

## Task 3 · Producer–consumer

- What breaks with `notify()` + `if` instead of `notifyAll()` + `while`, and why?

## Task 4 · Thread pools, Future and virtual threads

| Run | Result | Time (ms) |
|---|---|---|
| Primes, single-threaded | | |
| Primes, fixed pool | | |
| Downloads, fixed(20) platform | | |
| Downloads, virtual threads | | |

- Why are virtual threads faster for the I/O-bound work?

## Task 5 · Deadlock and graceful shutdown

- jstack report: see [`deadlock.txt`](deadlock.txt)
- OrderedBank result:
- TryLockBank result (retries):
- Graceful shutdown result:

## Challenge (optional) · Parallel word-frequency analyser

| Workers | Elapsed (s) |
|---|---|
| 1 platform | |
| 2 platform | |
| 4 platform | |
| 8 platform | |
| virtual | |

- Why LongAdder / `merge` beats `synchronized` on the map:
- Which Coffman conditions the design rules out, and how:
