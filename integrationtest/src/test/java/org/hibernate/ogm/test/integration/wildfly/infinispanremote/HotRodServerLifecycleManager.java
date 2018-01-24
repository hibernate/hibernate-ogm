/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.infinispanremote;

import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.spi.event.container.DeployerEvent;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Arquillian extension to start/stop the Hot Rod Server
 *
 * @author Sanne Grinovero
 */
public class HotRodServerLifecycleManager implements org.jboss.arquillian.core.spi.LoadableExtension {

	//N.B. the "100" as port offset for the server so that it can boot together with a standard WildFly instance:
	private final RemoteHotRodServerRule server = new RemoteHotRodServerRule( 100 );

	private int startedContainers = 0;

	public synchronized void startDatabase(@Observes BeforeDeploy event) {
		if ( ! hotRodRequired( event ) ) {
			return;
		}
		if ( startedContainers == 0 ) {
			try {
				server.before();
				System.out.println( "Hot Rod Server started" );
			}
			catch (Exception e) {
				System.err.println( "Failed to start Hot Rod Server" );
				e.printStackTrace();
			}
			startedContainers++;
		}
	}

	public synchronized void stopDatabase(@Observes AfterUnDeploy event) {
		if ( ! hotRodRequired( event ) ) {
			return;
		}
		startedContainers--;
		if ( startedContainers == 0 ) {
			server.after();
			System.out.println( "Hot Rod Server was shut down" );
		}
	}

	@Override
	public void register(ExtensionBuilder builder) {
		builder.observer( HotRodServerLifecycleManager.class );
	}

	private boolean hotRodRequired(DeployerEvent event) {
		Archive<?> archive = event.getDeployment().getArchive();
		return archive.contains( "/WEB-INF/classes/hotrod-client-configuration.properties" );
	}

}
