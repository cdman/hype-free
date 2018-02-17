package com.blogger.hypefree;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public final class GroupedThreadPool extends AbstractExecutorService {
	private final List<WorkerThread> workerThreads;
	private final ReentrantLock submitLock;

	public GroupedThreadPool(final int threadCount) {
		submitLock = new ReentrantLock();
		workerThreads = new ArrayList<WorkerThread>(threadCount);
		for (int i = 0; i < threadCount; ++i) {
			workerThreads.add(i, new WorkerThread());
			workerThreads.get(i).start();
		}
	}

	@Override
	public synchronized void shutdown() {
		// TODO: implement this such that multiple calls are properly handled
		final CountDownLatch shutDownLatch = submitWaitingTasks(workerThreads);
		try {
			shutDownLatch.await();
		} catch (InterruptedException e) {
			return;
		}

		for (WorkerThread workerThread : workerThreads) {
			workerThread.interrupt();
		}
	}

	@Override
	public List<Runnable> shutdownNow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTerminated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return true;
	}

	@Override
	public void execute(Runnable command) {
		if (command instanceof OrderedTask) {
			execute((OrderedTask) command);
		} else {
			execute(new RunnableWrapper(command));
		}
	}

	public void execute(final OrderedTask command) {
		submitLock.lock();
		try {
			for (WorkerThread workerThread : workerThreads) {
				workerThread.cleanFinishedTasks();
			}

			List<WorkerThread> conflictingWorkers = new ArrayList<WorkerThread>();
			for (WorkerThread workerThread : workerThreads) {
				if (!workerThread.isCompatible(command)) {
					conflictingWorkers.add(workerThread);
				}
			}

			if (conflictingWorkers.isEmpty()) {
				getShortersWorker().submitTask(command);
			} else if (conflictingWorkers.size() == 1) {
				conflictingWorkers.get(0).submitTask(command);
			} else {
				WorkerThread executingWorker = conflictingWorkers.remove(0);
				final CountDownLatch executionBarrier = submitWaitingTasks(conflictingWorkers);
				executingWorker.submitTask(new OrderedTask() {
					@Override
					public void run() {
						try {
							executionBarrier.await();
						} catch (InterruptedException e) {
							return;
						}
						command.run();
					}
					
					@Override
					public boolean isCompatible(OrderedTask that) {
						return command.isCompatible(that);
					}
				});
			}
		} finally {
			submitLock.unlock();
		}
	}

	private WorkerThread getShortersWorker() {
		assert submitLock.isHeldByCurrentThread();
		WorkerThread result = workerThreads.get(0);
		for (WorkerThread workerThread : workerThreads) {
			if (workerThread.getMirrorTasksSize() < result.getMirrorTasksSize()) {
				result = workerThread;
			}
		}
		return result;
	}

	private CountDownLatch submitWaitingTasks(
			Collection<WorkerThread> workerThreads) {
		final CountDownLatch result = new CountDownLatch(workerThreads.size());
		OrderedTask countDown = new OrderedTask() {
			@Override
			public void run() {
				result.countDown();
			}

			@Override
			public boolean isCompatible(OrderedTask that) {
				return true;
			}
		};

		for (WorkerThread workerThread : workerThreads) {
			workerThread.submitTask(countDown);
		}
		return result;
	}

	private final static class RunnableWrapper implements OrderedTask {
		private final Runnable runnable;

		RunnableWrapper(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run() {
			runnable.run();
		}

		@Override
		public boolean isCompatible(OrderedTask that) {
			return true;
		}
	}

	private final static class WorkerThread extends Thread {
		private final Queue<OrderedTask> tasks = new ConcurrentLinkedQueue<OrderedTask>();
		private final Semaphore taskSignal = new Semaphore(0);
		private final AtomicInteger finishedTasks = new AtomicInteger(0);
		private final LinkedList<OrderedTask> tasksMirror = new LinkedList<OrderedTask>();

		@Override
		public void run() {
			while (true) {
				try {
					taskSignal.acquire();
				} catch (InterruptedException e) {
					break;
				}
				Runnable runnable = tasks.poll();
				assert runnable != null;

				try {
					runnable.run();
				} catch (Exception ex) {
					// TODO: notify somebody about the exception
				} catch (Throwable t) {
					// TODO: notify somebody about the exception
					// TODO: restart the thread
					break;
				} finally {
					finishedTasks.incrementAndGet();
				}
			}
		}

		private void submitTask(OrderedTask task) {
			tasksMirror.addFirst(task);
			tasks.add(task);
			taskSignal.release();
		}

		private void cleanFinishedTasks() {
			int finishedTaskCount;
			while (true) {
				finishedTaskCount = finishedTasks.get();
				if (finishedTasks.compareAndSet(finishedTaskCount, 0)) {
					break;
				}
			}

			for (int i = 0; i < finishedTaskCount; ++i) {
				tasksMirror.removeLast();
			}
		}

		private boolean isCompatible(OrderedTask task) {
			for (OrderedTask mirrorTask : tasksMirror) {
				if (!mirrorTask.isCompatible(task)
						|| !task.isCompatible(mirrorTask)) {
					return false;
				}
			}
			return true;
		}

		private int getMirrorTasksSize() {
			return tasksMirror.size();
		}
	}
}
