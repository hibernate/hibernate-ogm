/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.loader.impl;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.loader.impl.OgmLoader;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

public class IgniteLoader extends OgmLoader {

	private final OgmEntityPersister[] entityPersisters;

	public IgniteLoader(OgmCollectionPersister[] collectionPersisters) {
		super( collectionPersisters );
		this.entityPersisters = new OgmEntityPersister[0];
	}

	public IgniteLoader(OgmEntityPersister[] entityPersisters, int batchSize) {
		super( entityPersisters, batchSize );
		this.entityPersisters = entityPersisters;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
		Object result = super.load( id, optionalObject, session, lockOptions );
		GridDialect gridDialect = entityPersisters[0].getFactory().getServiceRegistry().getService( GridDialect.class );
		if (lockOptions.getLockMode() == LockMode.PESSIMISTIC_READ && session.isTransactionInProgress() && entityPersisters.length > 0) {
			gridDialect.getLockingStrategy( entityPersisters[0], lockOptions.getLockMode() ).lock( id, null, result, lockOptions.getTimeOut(), session );
		}

		return result;
	}

}
