/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.dialect.impl;

import java.util.List;
import java.util.Arrays;

import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.orientdb.utils.EntityKeyUtil;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.hibernate.ogm.datastore.orientdb.utils.NativeQueryUtil;

/**
 * Container for the queries related to one entity type in OrientDB.
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBEntityQueries extends QueriesBase {

	private static Log log = LoggerFactory.getLogger();

	private final EntityKeyMetadata entityKeyMetadata;

	/**
	 * Contractor
	 *
	 * @param entityKeyMetadata metadata of entity keys
	 */
	public OrientDBEntityQueries(EntityKeyMetadata entityKeyMetadata) {
		this.entityKeyMetadata = entityKeyMetadata;
		for ( int i = 0; i < entityKeyMetadata.getColumnNames().length; i++ ) {
			String columnName = entityKeyMetadata.getColumnNames()[i];
			log.debugf( "column number: %d ; column name: %s", i, columnName );
		}
	}

	/**
	 * Find the node corresponding to the entity key.
	 *
	 * @param db current instance of db
	 * @param entityKey entity key
	 * @return the corresponding node
	 */

	public ODocument findEntity(ODatabaseDocumentTx db, EntityKey entityKey) {
		StringBuilder query = new StringBuilder( "SELECT FROM " );
		// search by business key
		log.debugf( "column names: %s", Arrays.asList( entityKey.getColumnNames() ) );
		query.append( entityKey.getTable() ).append( " WHERE " ).append( EntityKeyUtil.generatePrimaryKeyPredicate( entityKey ) );
		log.debugf( "find entiry query: %s", query.toString() );
		List<ODocument> documents = NativeQueryUtil.executeIdempotentQuery( db, query );
		if ( documents.isEmpty() ) {
			log.debugf( " entity by primary key %s not found!", entityKey );
			return null;
		}
		return documents.isEmpty() ? null : documents.get( 0 );
	}

	/**
	 * find association that corresponding to the association key.
	 *
	 * @param db connection to OrientDB
	 * @param associationKey association key
	 * @param associationContext context
	 * @return list of associations
	 */

	public List<ODocument> findAssociation(ODatabaseDocumentTx db, AssociationKey associationKey,
			AssociationContext associationContext) {
		log.debugf( "findAssociation: associationKey: %s; associationContext: %s", associationKey, associationContext );
		log.debugf( "findAssociation: associationKeyMetadata: %s", associationKey.getMetadata() );

		StringBuilder query = new StringBuilder( 100 );
		query.append( "SELECT FROM " ).append( associationKey.getTable() ).append( " WHERE " );
		for ( int i = 0; i < associationKey.getColumnNames().length; i++ ) {
			String name = associationKey.getColumnNames()[i];
			Object value = associationKey.getColumnValues()[i];
			query.append( name ).append( "=" );
			EntityKeyUtil.setFieldValue( query, value );
		}

		String[] indexColumns = associationKey.getMetadata().getRowKeyIndexColumnNames();
		if ( indexColumns != null && indexColumns.length > 0 ) {
			query.append( " order by " );
			for ( String indexColumn : indexColumns ) {
				query.append( indexColumn ).append( " asc " ).append( "," );
			}
			query.setLength( query.length() - 1 );
		}

		log.debugf( "findAssociation: query: %s", query );
		List<ODocument> documents = NativeQueryUtil.executeIdempotentQuery( db, query );
		log.debugf( "findAssociation: rows :  %d", documents.size() );
		return documents;
	}
}
