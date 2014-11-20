/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Default implementation of {@link SchemaDefiner}. Specific implementations can override those hooks they're
 * interested in. Also provides utility methods useful for implementors.
 *
 * @author Gunnar Morling
 */
public class BaseSchemaDefiner implements SchemaDefiner {

	@Override
	public void validateMapping(SessionFactoryImplementor factory) {
		// No-op
	}

	@Override
	public void initializeSchema(Configuration configuration, SessionFactoryImplementor factory) {
		// No-op
	}

	/**
	 * Returns all the persistent id generators which potentially require the creation of an object in the schema.
	 */
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

	protected Set<IdSourceKeyMetadata> getAllIdSourceKeyMetadata(SessionFactoryImplementor factory) {
		Set<IdSourceKeyMetadata> allIdSourceKeyMetadata = new HashSet<IdSourceKeyMetadata>();

		for ( PersistentNoSqlIdentifierGenerator generator : getPersistentGenerators( factory ) ) {
			allIdSourceKeyMetadata.add( generator.getGeneratorKeyMetadata() );
		}

		return allIdSourceKeyMetadata;
	}

	/**
	 * Returns the meta-data for all the entity types registered with the given session factory.
	 */
	protected Set<EntityKeyMetadata> getAllEntityKeyMetadata(SessionFactoryImplementor factory) {
		Set<EntityKeyMetadata> allEntityKeyMetadata = new HashSet<EntityKeyMetadata>();

		for ( EntityPersister entityPersister : factory.getEntityPersisters().values() ) {
			allEntityKeyMetadata.add( ( (OgmEntityPersister) entityPersister ).getEntityKeyMetadata() );
		}

		return allEntityKeyMetadata;
	}

	protected Set<AssociationKeyMetadata> getAllAssociationKeyMetadata(SessionFactoryImplementor factory) {
		Set<AssociationKeyMetadata> allAssociationKeyMetadata = new HashSet<AssociationKeyMetadata>();

		for ( CollectionPersister associationPersister : factory.getCollectionPersisters().values() ) {
			allAssociationKeyMetadata.add( ( (OgmCollectionPersister) associationPersister ).getAssociationKeyMetadata() );
		}

		for ( EntityPersister entityPersister : factory.getEntityPersisters().values() ) {
			for ( String property : entityPersister.getPropertyNames() ) {
				AssociationKeyMetadata inverseOneToOneAssociationKeyMetadata = ( (OgmEntityPersister) entityPersister ).getInverseOneToOneAssociationKeyMetadata( property );
				if ( inverseOneToOneAssociationKeyMetadata != null ) {
					allAssociationKeyMetadata.add( inverseOneToOneAssociationKeyMetadata );
				}
			}
		}

		return allAssociationKeyMetadata;
	}
}
