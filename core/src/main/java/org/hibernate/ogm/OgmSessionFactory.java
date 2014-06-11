/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Provides OGM-specific functionality on the session factory level.
 *
 * @author Gunnar Morling
 */
public interface OgmSessionFactory extends SessionFactoryImplementor {

	/**
	 * A {@link SessionBuilderImplementor} which creates {@link OgmSession}s.
	 *
	 * @author Gunnar Morling
	 *
	 */
	interface OgmSessionBuilderImplementor extends SessionBuilderImplementor {

		@Override
		OgmSession openSession();
	}

	@Override
	OgmSessionBuilderImplementor withOptions();

	/**
	 * Opens a new session based on the configured NoSQL datastore.
	 *
	 * @return A new Hibernate OGM session
	 */
	@Override
	OgmSession openSession() throws HibernateException;

	@Override
	OgmSession getCurrentSession() throws HibernateException;

	@Override
	OgmSession openTemporarySession() throws HibernateException;
}
