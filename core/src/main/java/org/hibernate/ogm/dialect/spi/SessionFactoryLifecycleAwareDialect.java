/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * A {@link GridDialect} implementing this interface may access to the different stages of a session factory life-cycle
 *
 * @author Davide D'Alto
 */
public interface SessionFactoryLifecycleAwareDialect extends GridDialect {

	/**
	 * Injection point for the dialect to receive the session factory.
	 *
	 * @param sessionFactoryImplementor a successfully created {@link SessionFactoryImplementor}
	 */
	void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor);
}
