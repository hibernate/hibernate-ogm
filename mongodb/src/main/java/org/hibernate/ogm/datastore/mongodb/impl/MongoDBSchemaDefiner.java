/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Performs sanity checks of the mapped objects.
 *
 * @author Gunnar Morling
 * @author Sanne Grinovero
 * @author Francois Le Droff
 * @author Guillaume Smet
 */
public class MongoDBSchemaDefiner extends BaseSchemaDefiner {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private MongoDBIndexesDefiner indexDefinitions = new MongoDBIndexesDefiner();

	@Override
	public void validateMapping(SchemaDefinitionContext context) {
		validateGenerators( context.getAllIdSourceKeyMetadata() );
		validateEntityCollectionNames( context.getAllEntityKeyMetadata() );
		validateAssociationNames( context.getAllAssociationKeyMetadata() );
		validateAllPersisters( context.getSessionFactory().getMetamodel().entityPersisters().values() );
		indexDefinitions.validateIndexSpecs( context );
	}

	@Override
	public void initializeSchema( SchemaDefinitionContext context) {
		SessionFactoryImplementor sessionFactoryImplementor = context.getSessionFactory();
		ServiceRegistryImplementor registry = sessionFactoryImplementor.getServiceRegistry();
		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) registry.getService( DatastoreProvider.class );
		indexDefinitions.createIndexes( provider.getDatabase() );
	}

	private void validateAllPersisters(Iterable<EntityPersister> persisters) {
		for ( EntityPersister persister : persisters ) {
			if ( persister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmPersister = (OgmEntityPersister) persister;
				int propertySpan = ogmPersister.getEntityMetamodel().getPropertySpan();
				for ( int i = 0; i < propertySpan; i++ ) {
					String[] columnNames = ogmPersister.getPropertyColumnNames( i );
					for ( String columnName : columnNames ) {
						validateAsMongoDBFieldName( columnName );
					}
				}
			}
		}
	}

	private void validateAssociationNames(Iterable<AssociationKeyMetadata> allAssociationKeyMetadata) {
		for ( AssociationKeyMetadata associationKeyMetadata : allAssociationKeyMetadata ) {
			validateAsMongoDBCollectionName( associationKeyMetadata.getTable() );
			for ( String column : associationKeyMetadata.getRowKeyColumnNames() ) {
				validateAsMongoDBFieldName( column );
			}
		}
	}

	private void validateEntityCollectionNames(Iterable<EntityKeyMetadata> allEntityKeyMetadata) {
		for ( EntityKeyMetadata entityKeyMetadata : allEntityKeyMetadata ) {
			validateAsMongoDBCollectionName( entityKeyMetadata.getTable() );
			for ( String column : entityKeyMetadata.getColumnNames() ) {
				validateAsMongoDBFieldName( column );
			}
		}
	}

	private void validateGenerators(Iterable<IdSourceKeyMetadata> allIdSourceKeyMetadata) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : allIdSourceKeyMetadata ) {
			String keyColumn = idSourceKeyMetadata.getKeyColumnName();

			if ( !keyColumn.equals( MongoDBDialect.ID_FIELDNAME ) ) {
				log.cannotUseGivenPrimaryKeyColumnName( keyColumn, MongoDBDialect.ID_FIELDNAME );
			}
		}
	}

	/**
	 * Validates a String to be a valid name to be used in MongoDB for a collection name.
	 *
	 * @param collectionName
	 */
	private static void validateAsMongoDBCollectionName(String collectionName) {
		Contracts.assertStringParameterNotEmpty( collectionName, "requestedName" );
		//Yes it has some strange requirements.
		if ( collectionName.startsWith( "system." ) ) {
			throw log.collectionNameHasInvalidSystemPrefix( collectionName );
		}
		else if ( collectionName.contains( "\u0000" ) ) {
			throw log.collectionNameContainsNULCharacter( collectionName );
		}
		else if ( collectionName.contains( "$" ) ) {
			throw log.collectionNameContainsDollarCharacter( collectionName );
		}
	}

	/**
	 * Validates a String to be a valid name to be used in MongoDB for a field name.
	 *
	 * @param fieldName
	 */
	private void validateAsMongoDBFieldName(String fieldName) {
		if ( fieldName.startsWith( "$" ) ) {
			throw log.fieldNameHasInvalidDollarPrefix( fieldName );
		}
		else if ( fieldName.contains( "\u0000" ) ) {
			throw log.fieldNameContainsNULCharacter( fieldName );
		}
	}
}
