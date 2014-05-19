/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Inialize {@link TypeTranslator}.
 *
 * This is a {@linl SessionFactoryServiceInitiator} since it depends on {@link DatastoreServices}
 * which itself is a {@code SessionFactoryServiceInitiator}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TypeTranslatorInitiator implements SessionFactoryServiceInitiator<TypeTranslator> {

	public static final TypeTranslatorInitiator INSTANCE = new TypeTranslatorInitiator();

	@Override
	public Class<TypeTranslator> getServiceInitiated() {
		return TypeTranslator.class;
	}

	@Override
	public TypeTranslator initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		return createService( registry );
	}

	@Override
	public TypeTranslator initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		return createService( registry );
	}

	private TypeTranslator createService(ServiceRegistryImplementor registry) {
		GridDialect dialect = registry.getService( GridDialect.class );
		return new TypeTranslatorImpl( dialect );
	}

}
