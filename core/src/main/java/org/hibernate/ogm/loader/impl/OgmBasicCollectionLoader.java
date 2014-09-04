/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.impl;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class OgmBasicCollectionLoader extends OgmLoader implements CollectionInitializer {
	public OgmBasicCollectionLoader(OgmCollectionPersister collectionPersister) {
		super( new OgmCollectionPersister[] { collectionPersister } );
	}

	@Override
	public void initialize(Serializable id, SessionImplementor session)
	throws HibernateException {
		loadCollection( session, id, getKeyType() );
	}

	protected Type getKeyType() {
		return getCollectionPersisters()[0].getKeyType();
	}
}
