/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ignite.binary.BinaryObject;
import org.hibernate.ogm.datastore.ignite.util.StringHelper;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.util.impl.CollectionHelper;

/**
 * @author Victor Kadachigov
 */
public class IgniteTupleSnapshot implements TupleSnapshot {

	private final EntityKeyMetadata keyMetadata;
	private final Object id;
	private final BinaryObject binaryObject;

	private final boolean isSimpleId;
	private final Set<String> columnNames;
	
//	private Set<String> embeddedCollectionsColumns;

	public IgniteTupleSnapshot(Object id, BinaryObject binaryObject, EntityKeyMetadata keyMetadata) {
		this.id = id;
		this.binaryObject = binaryObject;
		this.keyMetadata = keyMetadata;
		Set<String> idColumnNames = new HashSet<>();
		for ( String columnName : keyMetadata.getColumnNames() ) {
			if ( keyMetadata.isKeyColumn( columnName ) ) {
				idColumnNames.add( columnName );
			}
		}
		if ( idColumnNames.isEmpty() ) {
			throw new UnsupportedOperationException( "There is no id column in entity " + keyMetadata.getTable() + ". Hmm..." );
		}
		this.isSimpleId = idColumnNames.size() == 1;
		this.columnNames = CollectionHelper.asSet( keyMetadata.getColumnNames() ); 
//		this.columnNames = new HashSet<>(); 
//		Collections.addAll( this.columnNames, keyMetadata.getColumnNames() );
	}

	@Override
	public Object get(String column) {
		Object result = null;
		if ( !isEmpty() ) {
			if ( id != null /* not embedded collection item */  && keyMetadata.isKeyColumn( column ) ) {
				result = isSimpleId ? id : ( (BinaryObject) id ).field( StringHelper.stringAfterPoint( column ) );
			}
			else if ( binaryObject != null ) {
				result = binaryObject.field( StringHelper.realColumnName( column ) );
			}
		}
		return result;
	}

	@Override
	public boolean isEmpty() {
		return id == null && binaryObject == null;
	}

	@Override
	public Set<String> getColumnNames() {
		return columnNames;
	}

	/**
	 * @return key object in underlaying cache
	 */
	public Object getCacheKey() {
		return id;
	}

	/**
	 * @return value object in underlaying cache
	 */
	public BinaryObject getCacheValue() {
		return binaryObject;
	}
	
//	public void addEmbeddedCollectionColumn(String columnName) {
//		if ( embeddedCollectionsColumns == null ) {
//			embeddedCollectionsColumns = new HashSet<>();
//		}
//		embeddedCollectionsColumns.add( columnName );
//		columnNames.add( columnName );
//	}
}
