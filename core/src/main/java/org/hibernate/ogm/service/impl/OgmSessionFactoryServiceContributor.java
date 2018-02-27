/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.service.spi.SessionFactoryServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceRegistryBuilder;


/**
 * @author Guillaume Smet
 */
public class OgmSessionFactoryServiceContributor implements SessionFactoryServiceContributor {

	@Override
	public void contribute(SessionFactoryServiceRegistryBuilder serviceRegistryBuilder) {
		for ( SessionFactoryServiceInitiator<?> initiator : OgmSessionFactoryServiceInitiators.LIST ) {
			serviceRegistryBuilder.addInitiator( initiator );
		}
	}
}
