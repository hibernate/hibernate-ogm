/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.options.navigation.impl;

import org.hibernate.ogm.datastore.ehcache.options.navigation.EhcacheEntityContext;
import org.hibernate.ogm.datastore.ehcache.options.navigation.EhcacheGlobalContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStoreGlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts global Ehcache options.
 *
 * @author Gunnar Morling
 */
public abstract class EhcacheGlobalContextImpl extends BaseKeyValueStoreGlobalContext<EhcacheGlobalContext, EhcacheEntityContext> implements
		EhcacheGlobalContext {

	public EhcacheGlobalContextImpl(ConfigurationContext context) {
		super( context );
	}
}
