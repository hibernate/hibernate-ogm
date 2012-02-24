package org.hibernate.ogm.datastore.voldemort.impl;

import java.util.concurrent.LinkedBlockingQueue;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

public class TaskQueue extends LinkedBlockingQueue<Runnable> {

	private static final Log log = LoggerFactory.make();

	public boolean offer(Runnable runnable) {
		boolean res = super.offer(runnable);
		// log.info("just added: " + runnable + " current size: " + this.size()
		// + " added ?: " + res);
		return res;
	}

	public void runRunnable() {
		Runnable runnable = null;
		while ((runnable = this.poll()) != null) {
			// try {
			// log.info("running currently: " + (PutSequenceRunnable)
			// runnable);
			runnable.run();
			// } catch (Exception ex) {
			// log.error(ex.getCause());
			// }
		}
	}
}
