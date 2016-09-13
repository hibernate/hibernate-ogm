/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.options.navigation.impl;

import org.hibernate.ogm.datastore.infinispanremote.options.navigation.InfinispanRemoteEntityContext;
import org.hibernate.ogm.datastore.infinispanremote.options.navigation.InfinispanRemoteGlobalContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStoreGlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts global Infinispan Remote options.
 *
 * @author Gunnar Morling
 */
public abstract class InfinispanRemoteGlobalContextImpl extends BaseKeyValueStoreGlobalContext<InfinispanRemoteGlobalContext, InfinispanRemoteEntityContext> implements
		InfinispanRemoteGlobalContext {

	public InfinispanRemoteGlobalContextImpl(ConfigurationContext context) {
		super( context );
	}
}
