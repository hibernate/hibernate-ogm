/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes OGM's {@link DialectFactory} which always returns an instance of {@link OgmDialect}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmDialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {

	public static OgmDialectFactoryInitiator INSTANCE = new OgmDialectFactoryInitiator();

	@Override
	public DialectFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OgmDialectFactory( registry );
	}

	@Override
	public Class<DialectFactory> getServiceInitiated() {
		return DialectFactory.class;
	}

	private static class OgmDialectFactory implements DialectFactory {

		private final GridDialect gridDialect;

		public OgmDialectFactory(ServiceRegistryImplementor registry) {
			this.gridDialect = registry.getService( GridDialect.class );
		}

		@Override
		public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
			return new OgmDialect( gridDialect );
		}
	}
}
