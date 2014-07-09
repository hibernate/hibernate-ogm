/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmDialectFactoryInitiator extends OptionalServiceInitiator<DialectFactory> {

	public static OgmDialectFactoryInitiator INSTANCE = new OgmDialectFactoryInitiator();

	@Override
	protected DialectFactory buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		return new NoopDialectFactory();
	}

	@Override
	protected StandardServiceInitiator<DialectFactory> backupInitiator() {
		return DialectFactoryInitiator.INSTANCE;
	}

	@Override
	public Class<DialectFactory> getServiceInitiated() {
		return DialectFactory.class;
	}

	private static class NoopDialectFactory implements DialectFactory {

		@Override
		public Dialect buildDialect(Map configValues, DialectResolutionInfoSource resolutionInfoSource) throws HibernateException {
			return new NoopDialect();
		}
	}
}
