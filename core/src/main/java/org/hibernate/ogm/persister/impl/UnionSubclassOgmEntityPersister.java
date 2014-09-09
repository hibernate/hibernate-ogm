/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;

/**
 * Use table per class strategy.
 *
 * @see javax.persistence.InheritanceType#TABLE_PER_CLASS
 * @author "Davide D'Alto" &lt;davide@hibernate.org&gt;
 */
public class UnionSubclassOgmEntityPersister extends OgmEntityPersister {

	public UnionSubclassOgmEntityPersister(PersistentClass persistentClass,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy, SessionFactoryImplementor factory,
			Mapping mapping) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory, mapping,
				new TablePerClassDiscriminator( persistentClass ) );
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return null;
	}

}
