/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.batchindexing;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a blocking queue capable of storing
 * a "poison" token to signal consumer threads
 * that the task is finished.
 *
 * @author Sanne Grinovero
 */
public class ProducerConsumerQueue<T> {

	private static final int DEFAULT_BUFF_LENGHT = 1000;
	private static final Object EXIT_TOKEN = new Object();

	// doesn't use generics here as exitToken needs to be put in the queue too:
	private final BlockingQueue<Object> queue;
	private final AtomicInteger producersToWaitFor;

	/**
	 * @param producersToWaitFor
	 *            The number of producer threads.
	 */
	public ProducerConsumerQueue(int producersToWaitFor) {
		this( DEFAULT_BUFF_LENGHT, producersToWaitFor );
	}

	public ProducerConsumerQueue(int queueLenght, int producersToWaitFor) {
		this.queue = new ArrayBlockingQueue<Object>( queueLenght );
		this.producersToWaitFor = new AtomicInteger( producersToWaitFor );
	}

	/**
	 * Blocks until an object is available; when null
	 * is returned the client thread should quit.
	 *
	 * @return the next object in the queue, or null to exit
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public T take() throws InterruptedException {
		Object obj = queue.take();
		if ( obj == EXIT_TOKEN ) {
			// restore exit signal for other threads
			queue.put( EXIT_TOKEN );
			return null;
		}
		else {
			return (T) obj;
		}
	}

	/**
	 * Adds a new object to the queue, blocking if no space is
	 * available.
	 *
	 * @param obj
	 * @throws InterruptedException
	 */
	public void put(T obj) throws InterruptedException {
		queue.put( obj );
	}

	/**
	 * Each producer thread should call producerStopping() when it has
	 * finished. After doing it can safely terminate.
	 * After all producer threads have called producerStopping()
	 * a token will be inserted in the blocking queue to eventually
	 * awake sleeping consumers and have them quit, after the
	 * queue has been processed.
	 */
	public void producerStopping() {
		int activeProducers = producersToWaitFor.decrementAndGet();
		// last producer must close consumers
		if ( activeProducers == 0 ) {
			try {
				queue.put( EXIT_TOKEN );// awake all waiting threads to let them quit.
			}
			catch ( InterruptedException e ) {
				// just quit, consumers will be interrupted anyway if it's a shutdown.
				Thread.currentThread().interrupt();
			}
		}
	}

}
