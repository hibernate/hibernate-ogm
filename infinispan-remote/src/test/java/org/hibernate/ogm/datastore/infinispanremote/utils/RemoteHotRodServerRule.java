/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.utils;

import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STARTING;
import static org.jboss.as.controller.client.helpers.ClientConstants.CONTROLLER_PROCESS_STATE_STOPPING;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.launcher.Launcher;
import org.wildfly.core.launcher.StandaloneCommandBuilder;

/**
 * A JUnit rule which starts the Hot Rod server, and finally closes it.
 *
 * @author Sanne Grinovero
 */
public final class RemoteHotRodServerRule extends org.junit.rules.ExternalResource {

	private static final int MAX_WAIT_MILLISECONDS = 120 * 1000;
	private static final int STATE_REFRESH_MILLISECONDS = 50;
	private static final int MAX_STATE_REFRESH_ATTEMPTS =  MAX_WAIT_MILLISECONDS / STATE_REFRESH_MILLISECONDS;

	/**
	 * An atomic static flag to make it possible to reuse this class both as a global JUnit listener and as a Rule, and
	 * avoid starting the server twice.
	 */
	private static final AtomicBoolean running = new AtomicBoolean();

	private final int portOffset;

	/**
	 * Reference to the Hot Rod Server process. Access protected by synchronization on the static field "running".
	 */
	private Process hotRodServer;

	public RemoteHotRodServerRule() {
		this.portOffset = 0;
	}

	/**
	 * @param portOffset Allows to specify a port offset, equivalent to: -Djboss.socket.binding.port-offset=X
	 * This is needed when running multiple instances, or to run it in parallel with WildFly for integration tests.
	 */
	public RemoteHotRodServerRule(int portOffset) {
		this.portOffset = portOffset;
	}

	@Override
	public void before() throws Exception {
		// Synchronize on the static field to defend against concurrent launches,
		// e.g. the usage as JUnit Rule concurrently with the usage as global test listener in Surefire.
		synchronized ( running ) {
			if ( running.compareAndSet( false, true ) ) {
				String InfinispanVersion = RemoteCache.class.getPackage().getImplementationVersion();
				StandaloneCommandBuilder builder = StandaloneCommandBuilder
						.of( "target/node1/infinispan-server-" + InfinispanVersion );
				builder
					.setServerReadOnlyConfiguration( "wildfly-trimmed-config.xml" );
				if ( portOffset != 0 ) {
					builder.addJavaOption( "-Djboss.socket.binding.port-offset=" + portOffset );
				}
				Launcher launcher = Launcher.of( builder );
				hotRodServer = launcher.inherit().launch();
				waitForRunning();
			}
		}
	}

	@Override
	public void after() {
		synchronized ( running ) {
			if ( hotRodServer != null ) {
				running.set( false );
				hotRodServer.destroyForcibly();
				messageOut( "Server Killed" );
			}
		}
	}

	public void waitForRunning() throws Exception {
		try ( ModelControllerClient client = ModelControllerClient.Factory.create( "localhost", 9990 + portOffset ) ) {
			waitForServerBoot( client );
			waitForCacheManagerBoot( client );
		}
	}

	private void waitForServerBoot(ModelControllerClient client) {
		for ( int attempts = 0; attempts < MAX_STATE_REFRESH_ATTEMPTS; attempts++ ) {
			if ( isServerInRunningState( client ) ) {
				messageOut( "Server is now running" );
				return;
			}
			waitOrAbort();
		}
		timedOut();
	}

	private void waitForCacheManagerBoot(ModelControllerClient client) {
		for ( int attempts = 0; attempts < MAX_STATE_REFRESH_ATTEMPTS; attempts++ ) {
			if ( isCacheExists( client ) ) {
				messageOut( "CacheManager is now running" );
				return;
			}
			waitOrAbort();
		}
		timedOut();
	}

	private void waitOrAbort() {
		try {
			Thread.sleep( STATE_REFRESH_MILLISECONDS );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( "Interrupted while waiting for Hot Rod server to have booted successfully" );
		}
	}

	private void timedOut() {
		throw new RuntimeException( "Timed out while waiting for Hot Rod server to have booted successfully" );
	}

	private boolean isServerInRunningState(ModelControllerClient client) {
		try {
			ModelNode op = new ModelNode();
			op.get( OP ).set( READ_ATTRIBUTE_OPERATION );
			op.get( OP_ADDR ).setEmptyList();
			op.get( NAME ).set( "server-state" );

			ModelNode rsp = client.execute( op );
			return SUCCESS.equals( rsp.get( OUTCOME ).asString() )
					&& !CONTROLLER_PROCESS_STATE_STARTING.equals( rsp.get( RESULT ).asString() )
					&& !CONTROLLER_PROCESS_STATE_STOPPING.equals( rsp.get( RESULT ).asString() );
		}
		catch (RuntimeException rte) {
			throw rte;
		}
		catch (IOException ex) {
			return false;
		}
	}

	private boolean isCacheExists(ModelControllerClient client) {
		try {
			PathAddress pathAddress = PathAddress.pathAddress( SUBSYSTEM, "datagrid-infinispan" )
					.append( "cache-container", "clustered" );

			ModelNode op = new ModelNode();
			op.get( OP ).set( READ_ATTRIBUTE_OPERATION );
			op.get( OP_ADDR ).set( pathAddress.toModelNode() );
			op.get( NAME ).set( "cache-manager-status" );

			ModelNode resp = client.execute( op );
			return SUCCESS.equals( resp.get( OUTCOME ).asString() ) && "RUNNING".equals( resp.get( RESULT ).asString() );
		}
		catch (RuntimeException rte) {
			throw rte;
		}
		catch (IOException ex) {
			return false;
		}
	}

	public static void main(String[] args) throws Exception {
		RemoteHotRodServerRule rule = new RemoteHotRodServerRule();
		rule.before();
		rule.after();
	}

	private static void messageOut(String msg) {
		System.out.println( "\n***\tTest client helper: " + msg + "\t***\n" );
	}

}
