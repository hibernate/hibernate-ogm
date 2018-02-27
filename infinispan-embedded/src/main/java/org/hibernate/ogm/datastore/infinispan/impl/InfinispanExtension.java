/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl;

import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.ExternalizersIntegration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;

/**
 * Implements a Service which is autodiscovered by Infinispan during its initialization.
 * We use this to make sure that any CacheManager which is being initialized externally
 * (not by Hibernate OGM) yes is having us on classpath can pick up our custom
 * Externalizers.
 *
 * @author Sanne Grinovero
 */
public class InfinispanExtension extends AbstractModuleLifecycle {

	@Override
	public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalCfg) {
		ExternalizersIntegration.registerOgmExternalizers( globalCfg );
	}

}
