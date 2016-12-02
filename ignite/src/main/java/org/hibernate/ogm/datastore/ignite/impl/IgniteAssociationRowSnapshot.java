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
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.util.impl.CollectionHelper;

/**
 * @author Victor Kadachigov
 */
public class IgniteAssociationRowSnapshot implements TupleSnapshot {

	private final AssociationKeyMetadata associationMetadata;
	private final Object id;
	private final BinaryObject binaryObject;

	private final boolean isSimpleId;
	private final boolean thirdTableLink;
	private final Set<String> columnNames;

	public IgniteAssociationRowSnapshot(Object id, BinaryObject binaryObject, AssociationKeyMetadata associationMetadata) {
		this.id = id;
		this.binaryObject = binaryObject;
		this.associationMetadata = associationMetadata;
		this.thirdTableLink = !associationMetadata.getTable().equals(associationMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getTable());
		if (this.thirdTableLink) {
			Set<String> cn = new HashSet<>();
			Collections.addAll( cn, associationMetadata.getRowKeyColumnNames() );
			Collections.addAll( cn, associationMetadata.getAssociatedEntityKeyMetadata().getAssociationKeyColumns() );
			this.columnNames = Collections.unmodifiableSet( cn );
			this.isSimpleId = true; //vk: not used in this case
		}
		else {
			Set<String> idColumnNames = new HashSet<>();
			EntityKeyMetadata entityKeyMetadata = associationMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
			for ( String columnName : entityKeyMetadata.getColumnNames() ) {
				if ( entityKeyMetadata.isKeyColumn( columnName ) ) {
					idColumnNames.add( columnName );
				}
			}
			if ( idColumnNames.isEmpty() ) {
				throw new UnsupportedOperationException( "There is no id column in entity " + entityKeyMetadata.getTable() + ". Hmm..." );
			}
			this.columnNames = CollectionHelper.asSet( entityKeyMetadata.getColumnNames() );
			this.isSimpleId = idColumnNames.size() == 1;
		}
	}
	
	@Override
	public Object get(String column) {
		Object result = null;
		if ( !isEmpty() ) {
			if ( !thirdTableLink && associationMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata().isKeyColumn( column ) ) {
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
}
