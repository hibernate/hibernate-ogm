/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Default implementation of {@link SchemaInitializer}. Specific implementations can override those hooks they're
 * interested in. Also provides utility methods useful for implementors.
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

	protected Set<PersistentNoSqlIdentifierGenerator> getPersistentGenerators(SessionFactoryImplementor factory) {
		Map<String, EntityPersister> entityPersisters = factory.getEntityPersisters();

		Set<PersistentNoSqlIdentifierGenerator> persistentGenerators = new HashSet<PersistentNoSqlIdentifierGenerator>( entityPersisters.size() );
		for ( EntityPersister persister : entityPersisters.values() ) {
			if ( persister.getIdentifierGenerator() instanceof PersistentNoSqlIdentifierGenerator ) {
				persistentGenerators.add( (PersistentNoSqlIdentifierGenerator) persister.getIdentifierGenerator() );
			}
		}

		return persistentGenerators;
	}
}
