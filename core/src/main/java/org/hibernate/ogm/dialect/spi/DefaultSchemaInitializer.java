/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Default implementation of {@link SchemaInitializer}. Specific implementations can override those hooks they're
 * interested in.
 *
 * @author Gunnar Morling
 */
public class DefaultSchemaInitializer implements SchemaInitializer {

	@Override
	public void validateMapping(SessionFactoryImplementor factory) {
		// No-op
	}

	@Override
	public void initializeSchema(Configuration configuration, org.hibernate.engine.spi.SessionFactoryImplementor factory) {
		// No-op
	}
}
