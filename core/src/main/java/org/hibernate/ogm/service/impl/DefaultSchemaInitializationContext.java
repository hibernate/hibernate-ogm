/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.id.spi.PersistentNoSqlIdentifierGenerator;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * The one and only implementation of {@link SchemaDefiner.SchemaDefinitionContext}.
 *
 * @author Gunnar Morling
 */
public class DefaultSchemaInitializationContext implements SchemaDefiner.SchemaDefinitionContext {

	private final Database database;
	private final SessionFactoryImplementor factory;

	public DefaultSchemaInitializationContext(Database database, SessionFactoryImplementor factory) {
		this.database = database;
		this.factory = factory;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public Set<EntityKeyMetadata> getAllEntityKeyMetadata() {
		Set<EntityKeyMetadata> allEntityKeyMetadata = new HashSet<>();

		for ( EntityPersister entityPersister : factory.getMetamodel().entityPersisters().values() ) {
			allEntityKeyMetadata.add( ( (OgmEntityPersister) entityPersister ).getEntityKeyMetadata() );
		}

		return allEntityKeyMetadata;
	}

	@Override
	public Set<AssociationKeyMetadata> getAllAssociationKeyMetadata() {
		Set<AssociationKeyMetadata> allAssociationKeyMetadata = new HashSet<>();

		for ( CollectionPersister associationPersister : factory.getMetamodel().collectionPersisters().values() ) {
			allAssociationKeyMetadata.add( ( (OgmCollectionPersister) associationPersister ).getAssociationKeyMetadata() );
		}

		for ( EntityPersister entityPersister : factory.getMetamodel().entityPersisters().values() ) {
			for ( String property : entityPersister.getPropertyNames() ) {
				AssociationKeyMetadata inverseOneToOneAssociationKeyMetadata = ( (OgmEntityPersister) entityPersister ).getInverseOneToOneAssociationKeyMetadata( property );
				if ( inverseOneToOneAssociationKeyMetadata != null ) {
					allAssociationKeyMetadata.add( inverseOneToOneAssociationKeyMetadata );
				}
			}
		}

		return allAssociationKeyMetadata;
	}

	@Override
	public Set<IdSourceKeyMetadata> getAllIdSourceKeyMetadata() {
		Set<IdSourceKeyMetadata> allIdSourceKeyMetadata = new HashSet<IdSourceKeyMetadata>();

		for ( PersistentNoSqlIdentifierGenerator generator : getPersistentGenerators() ) {
			allIdSourceKeyMetadata.add( generator.getGeneratorKeyMetadata() );
		}

		return allIdSourceKeyMetadata;
	}

	@Override
	public Map<String, Class<?>> getTableEntityTypeMapping() {
		Map<String, Class<?>> mapping = new HashMap<>();
		Map<String, EntityPersister> entityPersisters = factory.getMetamodel().entityPersisters();

		for ( Entry<String, EntityPersister> entityPersisterEntry : entityPersisters.entrySet() ) {
			OgmEntityPersister entityPersister = (OgmEntityPersister) entityPersisterEntry.getValue();
			mapping.put( entityPersister.getEntityKeyMetadata().getTable(), entityPersister.getMappedClass() );
		}

		return mapping;
	}

	/**
	 * Returns all the persistent id generators which potentially require the creation of an object in the schema.
	 */
	private Iterable<PersistentNoSqlIdentifierGenerator> getPersistentGenerators() {
		Map<String, EntityPersister> entityPersisters = factory.getMetamodel().entityPersisters();

		Set<PersistentNoSqlIdentifierGenerator> persistentGenerators = new HashSet<PersistentNoSqlIdentifierGenerator>( entityPersisters.size() );
		for ( EntityPersister persister : entityPersisters.values() ) {
			if ( persister.getIdentifierGenerator() instanceof PersistentNoSqlIdentifierGenerator ) {
				persistentGenerators.add( (PersistentNoSqlIdentifierGenerator) persister.getIdentifierGenerator() );
			}
		}

		return persistentGenerators;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return factory;
	}
}
