/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes Hibernate OGM's {@link MutableIdentifierGeneratorFactory} implementation.
 *
 * @author Gunnar Morling
 */
public class OgmMutableIdentifierGeneratorFactoryInitiator implements StandardServiceInitiator<MutableIdentifierGeneratorFactory> {

	public static final OgmMutableIdentifierGeneratorFactoryInitiator INSTANCE = new OgmMutableIdentifierGeneratorFactoryInitiator();

	@Override
	public Class<MutableIdentifierGeneratorFactory> getServiceInitiated() {
		return MutableIdentifierGeneratorFactory.class;
	}

	@Override
	public MutableIdentifierGeneratorFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OgmMutableIdentifierGeneratorFactory();
	}
}
