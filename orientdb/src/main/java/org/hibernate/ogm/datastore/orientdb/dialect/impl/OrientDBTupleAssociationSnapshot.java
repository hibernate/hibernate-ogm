/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.orientdb.dialect.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.orientdb.dto.EmbeddedColumnInfo;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.orientdb.utils.EntityKeyUtil;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Represents a association tuple snapshot as loaded by the datastore.
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class OrientDBTupleAssociationSnapshot implements TupleSnapshot {

	private static final Log log = LoggerFactory.getLogger();
	private final AssociationContext associationContext;
	private final AssociationKey associationKey;
	private final Map<String, Object> properties = new LinkedHashMap<>();

	private final ODocument relationship;

	public OrientDBTupleAssociationSnapshot(ODocument relationship, AssociationKey associationKey, AssociationContext associationContext) {
		log.debug( "OrientDBTupleAssociationSnapshot: AssociationKey:" + associationKey + "; AssociationContext" + associationContext );
		this.relationship = relationship;
		this.associationKey = associationKey;
		this.associationContext = associationContext;
		collectProperties();
	}

	private void collectProperties() {

		String[] rowKeyColumnNames = associationKey.getMetadata().getRowKeyColumnNames();

		// Index columns
		for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
			String rowKeyColumn = rowKeyColumnNames[i];
			log.debug( "rowKeyColumn: " + rowKeyColumn + ";" );

			for ( int i1 = 0; i1 < associationKey.getColumnNames().length; i1++ ) {
				String columnName = associationKey.getColumnNames()[i1];
				log.debug( "columnName: " + columnName + ";" );
				if ( rowKeyColumn.equals( columnName ) ) {
					log.debug( "column value : " + associationKey.getColumnValue( columnName ) + ";" );
					properties.put( rowKeyColumn, associationKey.getColumnValue( columnName ) );
				}
			}
		}
		properties.putAll( relationship.toMap() );
		log.debug( "1.collectProperties: " + properties );
	}

	@Override
	public Object get(String columnName) {
		Object value = properties.get( columnName );
		if ( value == null && EntityKeyUtil.isEmbeddedColumn( columnName ) ) {
			EmbeddedColumnInfo ec = new EmbeddedColumnInfo( columnName );
			ODocument embeddedContainer = (ODocument) properties.get( ec.getClassNames().get( 0 ) );
			value = embeddedContainer.field( ec.getPropertyName() );
		}
		return value;
	}

	@Override
	public Set<String> getColumnNames() {
		return properties.keySet();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}
}
