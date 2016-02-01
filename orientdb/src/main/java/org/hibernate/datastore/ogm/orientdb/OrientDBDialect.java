/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.datastore.ogm.orientdb.dialect.impl.OrientDBAssociationQueries;
import org.hibernate.datastore.ogm.orientdb.dialect.impl.OrientDBEntityQueries;
import org.hibernate.datastore.ogm.orientdb.dialect.impl.OrientDBTupleSnapshot;
import org.hibernate.datastore.ogm.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.datastore.ogm.orientdb.type.spi.ORecordIdGridType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.SessionFactoryLifecycleAwareDialect;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import com.orientechnologies.orient.core.id.ORecordId;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class OrientDBDialect extends BaseGridDialect
		implements SessionFactoryLifecycleAwareDialect, IdentityColumnAwareGridDialect {

	private static final Log log = LoggerFactory.getLogger();

	private OrientDBDatastoreProvider provider;
	private Map<AssociationKeyMetadata, OrientDBAssociationQueries> associationQueries;
	private Map<EntityKeyMetadata, OrientDBEntityQueries> entityQueries;

	public OrientDBDialect(OrientDBDatastoreProvider provider) {
		this.provider = provider;
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		log.info( "getTuple:EntityKey:" + key + "; tupleContext" + tupleContext );
		ORecordId dbKey = null;
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			String columnName = key.getColumnNames()[i];
			Object columnValue = key.getColumnValues()[i];
			log.info( "EntityKey: columnName: " + columnName + ";columnValue: " + columnValue + " (class:" + columnValue.getClass().getName() + ");" );
			if ( columnName.equals( "@rid" ) ) {
				dbKey = (ORecordId) columnValue;
			}
		}

		if ( dbKey.getClusterPosition() == ORecordId.EMPTY_RECORD_ID.getClusterPosition() ) {
			// is is temporary value. no entity in db with this key
			log.info( "getTuple:Key:" + dbKey + "is temporary!" );
			return null;
		}

		try {
			Map<String, Object> dbValuesMap = entityQueries.get( key.getMetadata() ).findEntity( provider.getConnection(), key.getColumnValues() );
			return new Tuple(
					new OrientDBTupleSnapshot( dbValuesMap, tupleContext.getAllAssociatedEntityKeyMetadata(), tupleContext.getAllRoles(), key.getMetadata() ) );
		}
		catch (SQLException e) {
			log.error( "Can not find entity", e );
		}
		return null;
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		log.info( "createTuple:EntityKey:" + key + "; tupleContext" + tupleContext );
		return new Tuple( new OrientDBTupleSnapshot( key.getMetadata() ) );
	}

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) throws TupleAlreadyExistsException {
		log.info( "insertOrUpdateTuple:EntityKey:" + key + "; tupleContext" + tupleContext + "; tuple:" + tuple );
		Connection connection = provider.getConnection();

		StringBuilder queryBuffer = new StringBuilder();
		ORecordId dbKey = null;
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			String columnName = key.getColumnNames()[i];
			Object columnValue = key.getColumnValues()[i];
			log.info( "EntityKey: columnName: " + columnName + ";columnValue: " + columnValue + " (class:" + columnValue.getClass().getName() + ");" );
			if ( columnName.equals( "@rid" ) ) {
				dbKey = (ORecordId) columnValue;
			}
		}
		if ( dbKey.getClusterPosition() == ORecordId.EMPTY_RECORD_ID.getClusterPosition() ) {
			// it is new record =>insert a new record!
			log.info( "insertOrUpdateTuple:Key:" + dbKey + "is temporary!. Insert new record" );

			queryBuffer.append( "insert into " ).append( key.getTable() );
			if ( tuple.getColumnNames().size() > 1 ) {
				queryBuffer.append( " set " );
			}
			for ( String columnName : tuple.getColumnNames() ) {
				if ( columnName.equals( "@rid" ) ) {
					continue;
				}
				// @TODO correct type
				queryBuffer.append( " " ).append( columnName ).append( "='" ).append( tuple.get( columnName ) ).append( "'," );
			}
			queryBuffer.setLength( queryBuffer.length() - 1 );
			log.info( "insertOrUpdateTuple:Key:" + dbKey + ". insert query:" + queryBuffer.toString() );
			try {
				PreparedStatement pstmt = connection.prepareStatement( queryBuffer.toString() );
				log.info( "insertOrUpdateTuple:Key:" + dbKey + ". inserted: " + pstmt.executeUpdate() );
			}
			catch (SQLException e) {
				log.error( "Can not find entity", e );
				throw new RuntimeException( e );
			}

		}
		else {
			// it is old record =>update the record
			log.info( "insertOrUpdateTuple:Key:" + dbKey + "is persistent!. Update old record" );
		}
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		log.info( "insertTuple:EntityKeyMetadata:" + entityKeyMetadata + "; tupleContext" + tupleContext + "; tuple:" + tuple );
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		log.info( "removeTuple:EntityKey:" + key + "; tupleContext" + tupleContext );
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return true;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		log.info( "NextValueRequest:" + request + "; " );
		return ORecordId.EMPTY_RECORD_ID.getClusterPosition();
	}

	@Override
	public boolean supportsSequences() {
		return super.supportsSequences(); // To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	public void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor) {

		this.associationQueries = initializeAssociationQueries( sessionFactoryImplementor );
		this.entityQueries = initializeEntityQueries( sessionFactoryImplementor, associationQueries );
	}

	private Map<EntityKeyMetadata, OrientDBEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor,
			Map<AssociationKeyMetadata, OrientDBAssociationQueries> associationQueries) {
		Map<EntityKeyMetadata, OrientDBEntityQueries> entityQueries = initializeEntityQueries( sessionFactoryImplementor );
		for ( AssociationKeyMetadata associationKeyMetadata : associationQueries.keySet() ) {
			EntityKeyMetadata entityKeyMetadata = associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
			if ( !entityQueries.containsKey( entityKeyMetadata ) ) {
				// Embeddables metadata
				entityQueries.put( entityKeyMetadata, new OrientDBEntityQueries( entityKeyMetadata ) );
			}
		}
		return entityQueries;
	}

	private Map<EntityKeyMetadata, OrientDBEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<EntityKeyMetadata, OrientDBEntityQueries> queryMap = new HashMap<EntityKeyMetadata, OrientDBEntityQueries>();
		Collection<EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters().values();
		for ( EntityPersister entityPersister : entityPersisters ) {
			if ( entityPersister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) entityPersister;
				queryMap.put( ogmEntityPersister.getEntityKeyMetadata(), new OrientDBEntityQueries( ogmEntityPersister.getEntityKeyMetadata() ) );
			}
		}
		return queryMap;
	}

	private Map<AssociationKeyMetadata, OrientDBAssociationQueries> initializeAssociationQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<AssociationKeyMetadata, OrientDBAssociationQueries> queryMap = new HashMap<AssociationKeyMetadata, OrientDBAssociationQueries>();
		Collection<CollectionPersister> collectionPersisters = sessionFactoryImplementor.getCollectionPersisters().values();
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			if ( collectionPersister instanceof OgmCollectionPersister ) {
				OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) collectionPersister;
				EntityKeyMetadata ownerEntityKeyMetadata = ( (OgmEntityPersister) ( ogmCollectionPersister.getOwnerEntityPersister() ) ).getEntityKeyMetadata();
				AssociationKeyMetadata associationKeyMetadata = ogmCollectionPersister.getAssociationKeyMetadata();
				queryMap.put( associationKeyMetadata, new OrientDBAssociationQueries( ownerEntityKeyMetadata, associationKeyMetadata ) );
			}
		}
		return queryMap;
	}

	@Override
	public GridType overrideType(Type type) {
		log.info( "overrideType:" + type.getName() + ";" + type.getReturnedClass() );
		GridType gridType = null;
		if ( type.getName().equals( "com.orientechnologies.orient.core.id.ORecordId" ) ) {
			gridType = ORecordIdGridType.INSTANCE;
		}
		else {
			gridType = super.overrideType( type ); // To change body of generated methods, choose Tools | Templates.
		}
		return gridType;
	}

}
