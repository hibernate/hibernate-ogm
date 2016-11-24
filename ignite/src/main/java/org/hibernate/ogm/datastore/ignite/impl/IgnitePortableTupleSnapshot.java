/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

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
public class IgnitePortableTupleSnapshot implements TupleSnapshot {

	private final EntityKeyMetadata keyMetadata;
	private final Object id;
	private final BinaryObject binaryObject;

	private final boolean isSimpleId;
	private final Set<String> columnNames;
//	private final String idPrefix;

	public IgnitePortableTupleSnapshot(Object id, BinaryObject binaryObject, EntityKeyMetadata keyMetadata) {
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
			throw new UnsupportedOperationException( "There is not id column in entity " + keyMetadata.getTable() + ". Hmm..." );
		}
//		this.idPrefix = DocumentHelpers.getColumnSharedPrefix( idColumnNames.toArray( new String[ idColumnNames.size() ] ) );
		this.isSimpleId = idColumnNames.size() == 1;
		this.columnNames = CollectionHelper.asSet( keyMetadata.getColumnNames() );
	}

	@Override
	public Object get(String column) {
		Object result = null;
		if ( !isEmpty() ) {
			if ( keyMetadata.isKeyColumn( column ) ) {
				result = isSimpleId ? id : ( (BinaryObject) id ).field( StringHelper.stringAfterPoint( column ) );
			}
			else {
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

	public Object getKey() {
		return id;
	}

	public BinaryObject getValue() {
		return binaryObject;
	}
}
