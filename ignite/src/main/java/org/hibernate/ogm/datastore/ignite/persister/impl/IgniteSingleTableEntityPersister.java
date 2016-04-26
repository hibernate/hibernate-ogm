/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.persister.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.ogm.datastore.ignite.loader.impl.IgniteLoader;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.persister.impl.SingleTableOgmEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

public class IgniteSingleTableEntityPersister extends SingleTableOgmEntityPersister {

	public IgniteSingleTableEntityPersister(
			final PersistentClass persistentClass,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );
	}

	@Override
	protected UniqueEntityLoader createEntityLoader(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		return new IgniteLoader(new OgmEntityPersister[] { this }, batchSize);
	}

	@Override
	protected UniqueEntityLoader createEntityLoader(LockMode lockMode, LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException {
		return new IgniteLoader(new OgmEntityPersister[] { this }, batchSize);
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		// TODO: filters are not supported in OGM yet
		return "";
	}

	@Override
	public String filterFragment(String alias,  Map enabledFilters, Set<String> treatAsDeclarations) {
		// TODO: filters are not supported in OGM yet
		return "";
	}

	@Override
	public String filterFragment(String alias,  Map enabledFilters) {
		// TODO: filters are not supported in OGM yet
		return "";
	}

	@Override
	public String selectFragment(String alias, String suffix)
	{
		StringBuilder sb = new StringBuilder()
									.append(alias).append("._KEY, ")
									.append(alias).append("._VAL");
		
		return sb.toString();
	}
}
