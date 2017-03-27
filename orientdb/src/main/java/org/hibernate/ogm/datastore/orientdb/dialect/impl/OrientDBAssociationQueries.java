/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.dialect.impl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hibernate.AssertionFailure;

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.orientdb.utils.EntityKeyUtil;
import org.hibernate.ogm.datastore.orientdb.utils.NativeQueryUtil;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBAssociationQueries extends QueriesBase {

	private static final Log log = LoggerFactory.getLogger();

	private final EntityKeyMetadata ownerEntityKeyMetadata;
	private final AssociationKeyMetadata associationKeyMetadata;

	public OrientDBAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		this.ownerEntityKeyMetadata = ownerEntityKeyMetadata;
		this.associationKeyMetadata = associationKeyMetadata;
		log.debugf( "ownerEntityKeyMetadata: %s ;associationKeyMetadata: %s ",
				ownerEntityKeyMetadata, associationKeyMetadata );
	}

	public void removeAssociation(ODatabaseDocumentTx db, AssociationKey associationKey, AssociationContext associationContext) {
		log.debugf( "removeAssociation: %s ;associationKey: %s;", associationKey );
		log.debugf( "removeAssociation: AssociationKey: %s ; AssociationContext: %s", associationKey, associationContext );
		log.debugf( "removeAssociation: getAssociationKind: %s", associationKey.getMetadata().getAssociationKind() );
		StringBuilder deleteQuery = new StringBuilder( "SELECT FROM " );
		String columnName = null;
		log.debugf( "removeAssociation:%s", associationKey.getMetadata().getAssociationKind() );
		log.debugf( "removeAssociation:getRoleOnMainSide:%s", associationContext.getAssociationTypeContext().getRoleOnMainSide() );
		log.debugf( "removeAssociation:getAssociationType:%s", associationKey.getMetadata().getAssociationType() );
		log.debugf( "removeAssociation:AssociatedEntityKeyMetadata:%s", associationKey.getMetadata().getAssociatedEntityKeyMetadata() );
		switch ( associationKey.getMetadata().getAssociationKind() ) {
			case EMBEDDED_COLLECTION:
				deleteQuery.append( associationKey.getTable() ).append( " WHERE " );
				columnName = associationKey.getColumnNames()[0];
				deleteQuery.append( columnName ).append( "=" );
				EntityKeyUtil.setFieldValue( deleteQuery, associationKey.getColumnValues()[0] );
				break;
			case ASSOCIATION:
				String tableName = associationKey.getTable();
				log.debugf( "removeAssociation:getColumnNames:%s", Arrays.asList( associationKey.getColumnNames() ) );
				columnName = associationKey.getColumnNames()[0];
				deleteQuery.append( tableName ).append( " WHERE " );
				deleteQuery.append( columnName ).append( "=" );
				EntityKeyUtil.setFieldValue( deleteQuery, associationKey.getColumnValues()[0] );
				break;
			default:
				throw new AssertionFailure( "Unrecognized associationKind: " + associationKey.getMetadata().getAssociationKind() );
		}

		log.debugf( "removeAssociation: query for loading document for remove: %s ", deleteQuery );
		List<ODocument> removeDocs = NativeQueryUtil.executeIdempotentQuery( db, deleteQuery );
		for ( ODocument removeDoc : removeDocs ) {
			removeDoc.delete();
		}
		log.debugf( "removeAssociation: removed ssociations: %d ", removeDocs.size() );
	}

	public void removeAssociationRow(ODatabaseDocumentTx db, AssociationKey associationKey, RowKey rowKey) {
		log.debugf( "removeAssociationRow: associationKey: %s; RowKey:%s ", associationKey, rowKey );
		StringBuilder loadingDocsForDelete = new StringBuilder( 100 );
		loadingDocsForDelete.append( "SELECT FROM " ).append( associationKey.getTable() ).append( " WHERE " );
		for ( int i = 0; i < rowKey.getColumnNames().length; i++ ) {
			String columnName = rowKey.getColumnNames()[i];
			Object columnValue = rowKey.getColumnValues()[i];
			loadingDocsForDelete.append( columnName ).append( "=" );
			EntityKeyUtil.setFieldValue( loadingDocsForDelete, columnValue );
			if ( i < rowKey.getColumnNames().length - 1 ) {
				loadingDocsForDelete.append( " AND " );
			}
		}
		log.debugf( "removeAssociationRow: delete query: %s; ", loadingDocsForDelete );
		List<ODocument> removeDocs = NativeQueryUtil.executeIdempotentQuery( db, loadingDocsForDelete );
		for ( ODocument removeDoc : removeDocs ) {
			removeDoc.delete();
		}
		log.debugf( "removeAssociation: removed rows: %d ", removeDocs.size() );

	}

	public List<Map<String, Object>> findRelationship(ODatabaseDocumentTx db, AssociationKey associationKey, RowKey rowKey) {
		Map<String, Object> relationshipValues = new LinkedHashMap<>();
		log.debugf( "findRelationship: associationKey: %s", associationKey );
		log.debugf( "findRelationship: row key : %s", rowKey );
		log.debugf( "findRelationship: row key index columns: %d",
				associationKey.getMetadata().getRowKeyIndexColumnNames().length );
		log.debugf( "findRelationship: row key column names: %s",
				Arrays.asList( associationKey.getMetadata().getRowKeyColumnNames() ) );
		if ( associationKey.getMetadata().getRowKeyIndexColumnNames().length > 0 ) {
			String[] indexColumnNames = associationKey.getMetadata().getRowKeyIndexColumnNames();
			for ( int i = 0; i < indexColumnNames.length; i++ ) {
				for ( int j = 0; j < rowKey.getColumnNames().length; j++ ) {
					if ( indexColumnNames[i].equals( rowKey.getColumnNames()[j] ) ) {
						relationshipValues.put( rowKey.getColumnNames()[j], rowKey.getColumnValues()[j] );
					}
				}
			}
		}
		else if ( associationKey.getMetadata().getRowKeyColumnNames().length > 0 ) {
			String[] indexColumnNames = associationKey.getMetadata().getRowKeyColumnNames();
			for ( int i = 0; i < indexColumnNames.length; i++ ) {
				for ( int j = 0; j < rowKey.getColumnNames().length; j++ ) {
					if ( indexColumnNames[i].equals( rowKey.getColumnNames()[j] ) ) {
						relationshipValues.put( rowKey.getColumnNames()[j], rowKey.getColumnValues()[j] );
					}
				}
			}
		}
		else {
			EntityKey entityKey = getEntityKey( associationKey, rowKey );
			for ( int i = 0; i < entityKey.getColumnNames().length; i++ ) {
				relationshipValues.put( entityKey.getColumnNames()[i], entityKey.getColumnValues()[i] );
			}
		}
		for ( String columnName : associationKey.getColumnNames() ) {
			relationshipValues.put( columnName, associationKey.getColumnValue( columnName ) );
		}

		for ( Map.Entry<String, Object> entry : relationshipValues.entrySet() ) {
			String key = entry.getKey();
			Object value = entry.getValue();
			log.debugf( "findRelationship: key %s; value: %s", key, value );
		}
		List<Map<String, Object>> dbValues = new LinkedList<>();
		StringBuilder queryBuilder = new StringBuilder( 100 );
		queryBuilder.append( "SELECT FROM " ).append( associationKey.getTable() ).append( " WHERE " );
		int index = 0;
		for ( Map.Entry<String, Object> entry : relationshipValues.entrySet() ) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if ( index > 0 ) {
				queryBuilder.append( " AND " );
			}
			queryBuilder.append( key ).append( "=" );
			EntityKeyUtil.setFieldValue( queryBuilder, value );
			index++;
		}
		log.debugf( "findRelationship: queryBuilder: %s", queryBuilder );
		List<ODocument> documents = NativeQueryUtil.executeIdempotentQuery( db, queryBuilder );
		for ( ODocument doc : documents ) {
			dbValues.add( doc.toMap() );
		}
		log.debugf( "findRelationship: found: %d", dbValues.size() );
		return dbValues;
	}

	private EntityKey getEntityKey(AssociationKey associationKey, RowKey rowKey) {
		String[] associationKeyColumns = associationKey.getMetadata().getAssociatedEntityKeyMetadata().getAssociationKeyColumns();
		Object[] columnValues = new Object[associationKeyColumns.length];
		for ( int i = 0; i < associationKeyColumns.length; i++ ) {
			columnValues[i] = rowKey.getColumnValue( associationKeyColumns[i] );
		}
		EntityKeyMetadata entityKeyMetadata = associationKey.getMetadata().getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
		return new EntityKey( entityKeyMetadata, columnValues );
	}
}
