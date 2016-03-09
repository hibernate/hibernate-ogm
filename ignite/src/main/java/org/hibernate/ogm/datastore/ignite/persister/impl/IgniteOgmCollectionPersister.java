/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.persister.impl;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.spi.PersisterCreationContext;

public class IgniteOgmCollectionPersister extends OgmCollectionPersister {

	public IgniteOgmCollectionPersister(Collection collection,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			PersisterCreationContext persisterCreationContext)
			throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, persisterCreationContext );
	}

	@Override
	public String selectFragment(Joinable rhs, String rhsAlias, String lhsAlias, String currentEntitySuffix, String currentCollectionSuffix, boolean includeCollectionColumns) {
		// kozlow-ds: get from OneToManyPersister.selectFragment
		StringBuilder buf = new StringBuilder();
		if ( includeCollectionColumns ) {
//			buf.append( selectFragment( lhsAlias, "" ) )//ignore suffix for collection columns!
			buf.append( selectFragment( lhsAlias, currentCollectionSuffix ) )
					.append( ", " );
		}
		OuterJoinLoadable ojl = (OuterJoinLoadable) getElementPersister();
		return buf.append( ojl.selectFragment( lhsAlias, currentEntitySuffix ) )//use suffix for the entity columns
				.toString();
	}

	@Override
	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return StringUtils.defaultString( super.whereJoinFragment( alias, innerJoin, includeSubclasses ) );
	}

	@Override
	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses, Set<String> treatAsDeclarations) {
		return StringUtils.defaultString( super.whereJoinFragment( alias, innerJoin, includeSubclasses, treatAsDeclarations ) );
	}

	@Override
	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return StringUtils.defaultString( super.fromJoinFragment( alias, innerJoin, includeSubclasses ) );
	}

	@Override
	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses, Set<String> treatAsDeclarations) {
		return StringUtils.defaultString( super.fromJoinFragment( alias, innerJoin, includeSubclasses, treatAsDeclarations ) );
	}

	@Override
	public String filterFragment(String alias, Map enabledFilters) throws MappingException {
		return StringUtils.defaultString( super.filterFragment( alias, enabledFilters ) );
	}

	@Override
	public String filterFragment(String alias, Map enabledFilters, Set<String> treatAsDeclarations) throws MappingException {
		return StringUtils.defaultString( super.filterFragment( alias, enabledFilters, treatAsDeclarations ) );
	}

	@Override
	public String oneToManyFilterFragment(String alias) throws MappingException {
		return StringUtils.defaultString( super.oneToManyFilterFragment( alias ) );
	}

	@Override
	public String oneToManyFilterFragment(String alias, Set<String> treatAsDeclarations) {
		return StringUtils.defaultString( super.oneToManyFilterFragment( alias, treatAsDeclarations ) );
	}

	@Override
	public boolean consumesEntityAlias() {
		return true; //delegate.consumesEntityAlias();
	}

	@Override
	public boolean consumesCollectionAlias() {
		return true; //delegate.consumesCollectionAlias();
	}

}
