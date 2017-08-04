/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * An {@link EntityKeyMetadata} that keeps track of the discriminator value.
 * <p>
 * Entities in a hierarchy using single table inheritance strategy have the same {@link EntityKeyMetadata}, to make sure
 * that the metadata key is unique for each entity type we also keep track of the discriminator value.
 *
 * @author Davide D'Alto
 */
public class DiscriminatorAwareKeyMetadata implements EntityKeyMetadata {

	private final EntityKeyMetadata delegate;
	private final Object discriminatorValue;

	public DiscriminatorAwareKeyMetadata(EntityKeyMetadata delegate, Object discriminatorValue) {
		this.delegate = delegate;
		this.discriminatorValue = discriminatorValue;
	}

	@Override
	public String getTable() {
		return delegate.getTable();
	}

	@Override
	public String[] getColumnNames() {
		return delegate.getColumnNames();
	}

	@Override
	public boolean isKeyColumn(String columnName) {
		return delegate.isKeyColumn( columnName );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( delegate == null ) ? 0 : delegate.hashCode() );
		result = prime * result + ( ( discriminatorValue == null ) ? 0 : discriminatorValue.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		DiscriminatorAwareKeyMetadata other = (DiscriminatorAwareKeyMetadata) obj;
		if ( delegate == null ) {
			if ( other.delegate != null ) {
				return false;
			}
		}
		else if ( !delegate.equals( other.delegate ) ) {
			return false;
		}
		if ( discriminatorValue == null ) {
			if ( other.discriminatorValue != null ) {
				return false;
			}
		}
		else if ( !discriminatorValue.equals( other.discriminatorValue ) ) {
			return false;
		}
		return true;
	}
}
