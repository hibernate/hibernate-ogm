/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.impl;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.HibernateException;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.ogm.model.spi.AssociationOrderBy;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 * @author Fabio Massimo Ercoli
 */
public class OgmBasicCollectionLoader extends OgmLoader implements CollectionInitializer {
	public OgmBasicCollectionLoader(OgmCollectionPersister collectionPersister) {
		super( new OgmCollectionPersister[] { collectionPersister } );
	}

	@Override
	public void initialize(Serializable id, SharedSessionContractImplementor session)
			throws HibernateException {
		loadCollection( session, id, getKeyType() );
	}

	protected Type getKeyType() {
		return getCollectionPersisters()[0].getKeyType();
	}

	@Override
	protected void hydrateRowCollection(CollectionPersister persister, CollectionAliases descriptor, ResultSet rs, Object owner, PersistentCollection rowCollection)
			throws SQLException {

		List<AssociationOrderBy> orderBy = ( (OgmCollectionPersister) persister ).getAssociationKeyMetadata().getManyToManyOrderBy();
		if ( notNeedToHandleOrderBy( rowCollection, orderBy ) ) {
			super.hydrateRowCollection( persister, descriptor, rs, owner, rowCollection );
			return;
		}

		// handle @OrderBy
		PersistentBag bag = (PersistentBag) rowCollection;
		SharedSessionContractImplementor session = bag.getSession();

		final Object element = persister.readElement( rs, owner, descriptor.getSuffixedElementAliases(), session ) ;
		if ( element == null ) {
			return;
		}

		boolean inserted = false;
		ListIterator li = bag.listIterator();
		while ( li.hasNext() ) {
			Object next = li.next();
			int compare = AssociationOrderBy.compareWithOrderChain( element, next, orderBy );

			if ( compare < 0 ) {
				// swap element with next
				li.remove();
				li.add( element );
				// then insert next after it
				li.add( next );

				inserted = true;
				break;
			}
		}

		if ( !inserted ) {
			bag.add( element );
		}
	}

	private boolean notNeedToHandleOrderBy(PersistentCollection rowCollection, List<AssociationOrderBy> orderBy) {
		return orderBy == null || !( rowCollection instanceof PersistentBag );
	}
}
