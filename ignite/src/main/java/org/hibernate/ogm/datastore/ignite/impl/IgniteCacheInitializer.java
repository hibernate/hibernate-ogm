/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Arrays;

import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.configuration.CacheConfiguration;
import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.util.StringHelper;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Victor Kadachigov
 */
public class IgniteCacheInitializer extends BaseSchemaDefiner {

	private static final long serialVersionUID = -8564869898957031491L;
	private static final Log log = LoggerFactory.getLogger();

	@Override
	public void initializeSchema(SchemaDefinitionContext context) {

		DatastoreProvider provider = context.getSessionFactory().getServiceRegistry().getService( DatastoreProvider.class );
		if ( provider instanceof IgniteDatastoreProvider ) {
			IgniteDatastoreProvider igniteDatastoreProvider = (IgniteDatastoreProvider) provider;
			initializeEntities( context, igniteDatastoreProvider );
			initializeAssociations( context, igniteDatastoreProvider );
			initializeIdSources( context, igniteDatastoreProvider );
		}
		else {
			log.unexpectedDatastoreProvider( provider.getClass(), IgniteDatastoreProvider.class );
		}
	}

	private void initializeEntities(SchemaDefinitionContext context, IgniteDatastoreProvider igniteDatastoreProvider) {
		for ( EntityKeyMetadata entityKeyMetadata : context.getAllEntityKeyMetadata() ) {
			try {
				try {
					igniteDatastoreProvider.getEntityCache( entityKeyMetadata );
				}
				catch (HibernateException ex) {
					CacheConfiguration config = createCacheConfiguration( entityKeyMetadata, context );
					igniteDatastoreProvider.initializeCache( config );
				}
			}
			catch (Exception ex) {
				// just write error to log
				log.warn( log.unableToInitializeCache( entityKeyMetadata.getTable() ), ex );
			}
		}
	}

	private void initializeAssociations(SchemaDefinitionContext context, IgniteDatastoreProvider igniteDatastoreProvider) {
		for ( AssociationKeyMetadata associationKeyMetadata : context.getAllAssociationKeyMetadata() ) {
			if ( associationKeyMetadata.getAssociationKind() != AssociationKind.EMBEDDED_COLLECTION
					&& IgniteAssociationSnapshot.isThirdTableAssociation( associationKeyMetadata ) ) {
				try {
					try {
						igniteDatastoreProvider.getAssociationCache( associationKeyMetadata );
					}
					catch (HibernateException ex) {
						CacheConfiguration config = createCacheConfiguration( associationKeyMetadata, context );
						if ( config != null ) {
							igniteDatastoreProvider.initializeCache( config );
						}
					}
				}
				catch (Exception ex) {
					// just write error to log
					log.warn( log.unableToInitializeCache( associationKeyMetadata.getTable() ), ex );
				}

			}
		}
	}

	private void initializeIdSources(SchemaDefinitionContext context, IgniteDatastoreProvider igniteDatastoreProvider) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : context.getAllIdSourceKeyMetadata() ) {
			if ( idSourceKeyMetadata.getType() == IdSourceType.TABLE ) {
				try {
					try {
						igniteDatastoreProvider.getIdSourceCache( idSourceKeyMetadata );
					}
					catch (HibernateException ex) {
						CacheConfiguration config = createCacheConfiguration( idSourceKeyMetadata );
						igniteDatastoreProvider.initializeCache( config );
					}
				}
				catch (Exception ex) {
					// just write error to log
					log.warn( log.unableToInitializeCache( idSourceKeyMetadata.getName() ), ex );
				}
			}
		}
	}

	private CacheConfiguration createCacheConfiguration(IdSourceKeyMetadata idSourceKeyMetadata) {
		CacheConfiguration result = new CacheConfiguration();
		result.setName( StringHelper.stringBeforePoint( idSourceKeyMetadata.getName() ) );
		return result;
	}

	private CacheConfiguration createCacheConfiguration(AssociationKeyMetadata associationKeyMetadata, SchemaDefinitionContext context) {

		if ( associationKeyMetadata.getColumnNames().length > 1 ) {
			//composite id. not yet implemented
			return null;
		}

		CacheConfiguration result = new CacheConfiguration();
		result.setName( StringHelper.stringBeforePoint( associationKeyMetadata.getTable() ) );

		QueryEntity queryEntity = new QueryEntity();
		queryEntity.setValueType( StringHelper.stringAfterPoint( associationKeyMetadata.getTable() ) );
		appendIndex( queryEntity, associationKeyMetadata, context );

		result.setQueryEntities( Arrays.asList( queryEntity ) );

		return result;
	}

	private void appendIndex(QueryEntity queryEntity, AssociationKeyMetadata associationKeyMetadata, SchemaDefinitionContext context) {

		if ( associationKeyMetadata.getColumnNames().length > 1 ) {
			//composite id. not yet implemented
			return;
		}

		String idFieldName = associationKeyMetadata.getColumnNames()[0];
		String idClassName = getEntityIdClassName( associationKeyMetadata.getEntityKeyMetadata().getTable(), context );
		queryEntity.addQueryField( idFieldName, idClassName, null );
		queryEntity.setIndexes( Arrays.asList( new QueryIndex( idFieldName, QueryIndexType.SORTED ) ) );
	}

	private String getEntityIdClassName( String table, SchemaDefinitionContext context ) {
		Class<?> entityClass = context.getTableEntityTypeMapping().get( table );
		EntityPersister entityPersister = context.getSessionFactory().getEntityPersister( entityClass.getName() );
		Class<?> idClass = entityPersister.getIdentifierType().getReturnedClass();
		return idClass.getName();
	}

	private CacheConfiguration createCacheConfiguration(EntityKeyMetadata entityKeyMetadata, SchemaDefinitionContext context) {
		CacheConfiguration result = new CacheConfiguration();
		result.setName( StringHelper.stringBeforePoint( entityKeyMetadata.getTable() ) );
		result.setAtomicityMode( CacheAtomicityMode.TRANSACTIONAL );

		QueryEntity queryEntity = new QueryEntity();
		queryEntity.setKeyType( getEntityIdClassName( entityKeyMetadata.getTable(), context ) );
		queryEntity.setValueType( StringHelper.stringAfterPoint( entityKeyMetadata.getTable() ) );
		for ( AssociationKeyMetadata associationKeyMetadata : context.getAllAssociationKeyMetadata() ) {
			if ( associationKeyMetadata.getAssociationKind() != AssociationKind.EMBEDDED_COLLECTION
					&& associationKeyMetadata.getTable().equals( entityKeyMetadata.getTable() )
					&& !IgniteAssociationSnapshot.isThirdTableAssociation( associationKeyMetadata ) ) {
				appendIndex( queryEntity, associationKeyMetadata, context );
			}
		}
		result.setQueryEntities( Arrays.asList( queryEntity ) );
		return result;
	}
}
