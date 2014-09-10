/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;

/**
 * Performs sanity checks of the mapped objects.
 *
 * @author Gunnar Morling
 */
public class MongoDBEntityMappingValidator extends BaseSchemaDefiner {

	private static final Log log = LoggerFactory.getLogger();

	@Override
	public void validateMapping(SessionFactoryImplementor factory) {
		Set<PersistentNoSqlIdentifierGenerator> persistentGenerators = getPersistentGenerators( factory );
		validateGenerators( persistentGenerators );
	}

	private void validateGenerators(Iterable<PersistentNoSqlIdentifierGenerator> generators) {
		for ( PersistentNoSqlIdentifierGenerator identifierGenerator : generators ) {
			String keyColumn = identifierGenerator.getGeneratorKeyMetadata().getKeyColumnName();

			if ( !keyColumn.equals( MongoDBDialect.ID_FIELDNAME ) ) {
				log.cannotUseGivenPrimaryKeyColumnName( keyColumn, MongoDBDialect.ID_FIELDNAME );
			}
		}
	}
}
