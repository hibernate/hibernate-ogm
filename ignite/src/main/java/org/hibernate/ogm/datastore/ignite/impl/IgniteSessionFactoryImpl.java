/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.ignite.IgniteDialect;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.impl.OgmDialect;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionImpl;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

public class IgniteSessionFactoryImpl extends OgmSessionFactoryImpl {

	private static final long serialVersionUID = -438306758993171394L;
	private static final Log log = LoggerFactory.getLogger();

	public IgniteSessionFactoryImpl(SessionFactoryImplementor delegate) {
		super( delegate );
	}

	@Override
	public OgmSession openSession() throws HibernateException {
		final Session session = super.openSession();
		return createIgniteSession( session );
	}

	@Override
	public OgmSession getCurrentSession() throws HibernateException {
		final Session session = super.getCurrentSession();
		return createIgniteSession( session );
	}

	private IgniteSessionImpl createIgniteSession(Session session) {
		initCaches();
		return new IgniteSessionImpl(this, ((OgmSessionImpl)session).getDelegate());
	}

	public void initCaches() {
		Set<EntityKeyMetadata> cachesInfo = new HashSet<>();
		for (ClassMetadata classMetadata : getAllClassMetadata().values()) {
			cachesInfo.add( ((OgmEntityPersister) classMetadata).getEntityKeyMetadata() );
		}

		if (((OgmDialect) getDialect()).getGridDialect() instanceof IgniteDialect) {
			IgniteDialect dialect = (IgniteDialect) ((OgmDialect) getDialect()).getGridDialect();
			dialect.loadCache( cachesInfo );
		}
		else {
			log.warn( "GridDialect is not instance of IgniteDialect: " + ((OgmDialect) getDialect()).getGridDialect() );
		}
	}

}
