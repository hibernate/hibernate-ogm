/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.SessionFactoryLifecycleAwareDialect;

/**
 * Passes a successfully created session factory to a {@link GridDialect} if it implements the
 * {@link SessionFactoryLifecycleAwareDialect} interface.
 *
 * @author Davide D'Alto
 */
public class SessionFactoryLifecycleAwareDialectInitializer implements SessionFactoryObserver {

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		SessionFactoryImplementor factoryImplementor = (SessionFactoryImplementor) factory;
		GridDialect gridDialect = factoryImplementor.getServiceRegistry().getService( GridDialect.class );
		SessionFactoryLifecycleAwareDialect sessionFactoryAwareDialect = GridDialects.getDialectFacetOrNull( gridDialect, SessionFactoryLifecycleAwareDialect.class );
		if ( sessionFactoryAwareDialect != null ) {
			sessionFactoryAwareDialect.sessionFactoryCreated( factoryImplementor );
		}
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
	}
}
