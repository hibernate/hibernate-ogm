/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Initializes {@link TypeTranslator}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class TypeTranslatorInitiator implements SessionFactoryServiceInitiator<TypeTranslator> {

	public static final TypeTranslatorInitiator INSTANCE = new TypeTranslatorInitiator();

	@Override
	public Class<TypeTranslator> getServiceInitiated() {
		return TypeTranslator.class;
	}

	@Override
	public TypeTranslator initiateService(SessionFactoryImplementor sessionFactory, SessionFactoryOptions sessionFactoryOptions, ServiceRegistryImplementor registry) {
		GridDialect dialect = registry.getService( GridDialect.class );
		return new TypeTranslatorImpl( dialect, sessionFactory.getTypeResolver() );
	}
}
