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
import org.hibernate.ogm.dialect.spi.SessionFactoryAwareDialect;

/**
 * Passes a successfully created session factory to a {@link GridDialect} if it implements the
 * {@link SessionFactoryAwareDialect} interface.
 *
 * @author Davide D'Alto
 */
public class SessionFactoryAwareInitializer implements SessionFactoryObserver {

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		SessionFactoryImplementor factoryImplementor = (SessionFactoryImplementor) factory;
		GridDialect gridDialect = factoryImplementor.getServiceRegistry().getService( GridDialect.class );
		SessionFactoryAwareDialect sessionFactoryAwareDialect = GridDialects.getDialectFacetOrNull( gridDialect, SessionFactoryAwareDialect.class );
		if ( sessionFactoryAwareDialect != null ) {
			sessionFactoryAwareDialect.sessionFactoryCreated( factoryImplementor );
		}
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
	}
}
