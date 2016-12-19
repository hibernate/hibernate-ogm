/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.persister.impl;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.ogm.persister.impl.SingleTableOgmEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * @author Victor Kadachigov
 */
public class IgniteSingleTableEntityPersister extends SingleTableOgmEntityPersister {

	public IgniteSingleTableEntityPersister(
			final PersistentClass persistentClass,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		//vk: wrong implementation in base class
		return "";
	}
}
