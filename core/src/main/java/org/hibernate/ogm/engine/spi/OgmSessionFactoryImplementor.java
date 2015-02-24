/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.engine.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;

/**
 * OGM-specific extensions to {@link SessionFactoryImplementor}.
 *
 * @author Gunnar Morling
 */
public interface OgmSessionFactoryImplementor extends OgmSessionFactory, SessionFactoryImplementor {

	@Override
	OgmSessionBuilderImplementor withOptions();

	@Override
	OgmSession openTemporarySession() throws HibernateException;
}
