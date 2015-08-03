/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.impl;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilder;
import org.hibernate.ogm.service.impl.OgmConfigurationService;

/**
 * A {@link SessionFactoryBuilderFactory} return an {@link OgmSessionFactoryBuilder} for exposing OGM-specific options
 * and functionality.
 *
 * @author Gunnar Morling
 */
public class OgmSessionFactoryBuilderFactory implements SessionFactoryBuilderFactory {

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryBuilderImplementor defaultBuilder) {
		if ( !metadata.getMetadataBuildingOptions().getServiceRegistry().getService( OgmConfigurationService.class ).isOgmEnabled() ) {
			return defaultBuilder;
		}

		return new OgmSessionFactoryBuilderImpl( metadata, defaultBuilder );
	}
}
