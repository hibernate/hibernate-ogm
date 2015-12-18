/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.utils;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunListener.ThreadSafe;

/**
 * This JUnit listener is registered in the Maven build as a global listener,
 * making sure that the Hot Rod server is running and ready to serve
 * requests when running the testsuite.
 * As an alternative RemoteHotRodServerRule can be used as a JUnit Rule.
 *
 * @author Sanne Grinovero
 */
@ThreadSafe
public class HotrodServerLifecycle extends RunListener {

	private volatile RemoteHotRodServerRule server;

	@Override
	public void testRunStarted(Description description) throws Exception {
		printEvent( "Test suite start detected" );
		if ( server == null ) {
			startHotRodServer();
		}
	}

	@Override
	public void testRunFinished(Result result) throws Exception {
		if ( server != null ) {
			printEvent( "Test suite end detected" );
			shutdownServer();
		}
	}

	@Override
	public void testFinished(Description description) throws Exception {
		if ( server != null ) {
			// I'd like to do this, but in some cases currently OGM's tests
			// are bit always fully independent in the same class.
			// Not least, this would wipe out the schema.
			//	resetServer();
		}
	}

	private synchronized void resetServer() throws Exception {
		if ( server != null ) {
			server.resetHotRodServerState();
			printEvent( "Hot Rod server has been reset" );
		}
	}

	private synchronized void startHotRodServer() {
		if ( server == null ) {
			server = new RemoteHotRodServerRule();
			try {
				printEvent( "Starting HotRod Server" );
				server.before();
			}
			catch (Throwable e) {
				throw new RuntimeException( e );
			}
		}
	}

	private synchronized void shutdownServer() {
		final RemoteHotRodServerRule serverLocal = server;
		if ( serverLocal != null ) {
			printEvent( "Terminating HotRod Server" );
			serverLocal.after();
		}
	}

	private void printEvent(String message) {
		System.out.println( getClass().getCanonicalName() + ": " + message );
	}

}
