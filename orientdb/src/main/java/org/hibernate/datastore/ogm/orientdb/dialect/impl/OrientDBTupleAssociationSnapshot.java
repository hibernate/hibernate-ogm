/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 * 
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.datastore.ogm.orientdb.dialect.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.hibernate.datastore.ogm.orientdb.dto.Edge;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.model.key.spi.AssociationKind;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */

public class OrientDBTupleAssociationSnapshot implements TupleSnapshot {

	private static Log log = LoggerFactory.getLogger();
	private Edge relationship;
	private AssociationKey associationKey;
	private AssociationContext associationContext;
	private final Map<String, Object> properties;

	public OrientDBTupleAssociationSnapshot(Edge relationship, AssociationKey associationKey, AssociationContext associationContext) {
		log.info( "OrientDBTupleAssociationSnapshot: AssociationKey:" + associationKey + "; AssociationContext" + associationContext );
		this.relationship = relationship;
		this.associationKey = associationKey;
		this.associationContext = associationContext;
		properties = collectProperties();
	}

	private Map<String, Object> collectProperties() {
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		String[] rowKeyColumnNames = associationKey.getMetadata().getRowKeyColumnNames();

		ODocument ownerNode = relationship.getOut();

		for ( int i = 0; i < ownerNode.fields(); i++ ) {
			log.info( "owner field: " + ownerNode.fieldNames()[i] );
		}

		ODocument targetNode = relationship.getIn();

		// Index columns
		for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
			String rowKeyColumn = rowKeyColumnNames[i];
			log.info( "rowKeyColumn: " + rowKeyColumn + ";" );

			for ( int i1 = 0; i1 < associationKey.getColumnNames().length; i1++ ) {
				String columnName = associationKey.getColumnNames()[i1];
				log.info( "columnName: " + columnName + ";" );
				if ( rowKeyColumn.equals( columnName ) ) {
					log.info( "column value : " + associationKey.getColumnValue( columnName ) + ";" );
					properties.put( rowKeyColumn, associationKey.getColumnValue( columnName ) );
				}
			}

			for ( int j = 0; j < ownerNode.fields(); j++ ) {
				if ( ownerNode.fieldNames()[j].equals( rowKeyColumn ) ) {
					properties.put( rowKeyColumn, ownerNode.field( rowKeyColumn ) );
				}
			}
		}

		log.info( "1.collectProperties: " + properties );

		// Properties stored in the target side of the association
		/*
		 * AssociatedEntityKeyMetadata associatedEntityKeyMetadata =
		 * associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata(); for ( String
		 * associationColumn : associatedEntityKeyMetadata.getAssociationKeyColumns() ) { String targetColumnName =
		 * associatedEntityKeyMetadata.getCorrespondingEntityKeyColumn( associationColumn ); if (
		 * targetNode.containsField( targetColumnName ) ) { properties.put( associationColumn,
		 * targetNode.getOriginalValue( targetColumnName ) ); } }
		 */

		// Property stored in the owner side of the association
		/*
		 * for ( int i = 0; i < associationKey.getColumnNames().length; i++ ) { if ( ownerNode.containsField(
		 * associationKey.getEntityKey().getColumnNames()[i] ) ) { properties.put( associationKey.getColumnNames()[i],
		 * ownerNode.getOriginalValue(associationKey.getEntityKey().getColumnNames()[i] ) ); } }
		 */
		log.info( "collectProperties: " + properties );
		return properties;
	}

	private static boolean isEmbeddedCollection(AssociationKey associationKey) {
		return associationKey.getMetadata().getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION;
	}

	@Override
	public Object get(String column) {
		log.info( "targetColumnName: " + column );
		return properties.get( column );
	}

	@Override
	public boolean isEmpty() {
		log.info( "isEmpty " );
		return properties.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		log.info( "getColumnNames " );
		return properties.keySet();
	}

}
