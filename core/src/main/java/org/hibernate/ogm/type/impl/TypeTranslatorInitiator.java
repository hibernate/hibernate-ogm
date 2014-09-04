/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initializes {@link TypeTranslator}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class TypeTranslatorInitiator implements StandardServiceInitiator<TypeTranslator> {

	public static final TypeTranslatorInitiator INSTANCE = new TypeTranslatorInitiator();

	@Override
	public Class<TypeTranslator> getServiceInitiated() {
		return TypeTranslator.class;
	}

	@Override
	public TypeTranslator initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		GridDialect dialect = registry.getService( GridDialect.class );
		return new TypeTranslatorImpl( dialect );
	}
}
