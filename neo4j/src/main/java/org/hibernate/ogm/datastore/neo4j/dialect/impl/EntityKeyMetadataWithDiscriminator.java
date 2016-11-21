/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * This class is an {@link EntityKeyMetadata} that kepp tracks of the discriminator value required to keep track of
 * entities whne dealing with single table per class inheritance.
 * <p>
 * Normally, Hibernate ORM add a discriminator column and use the value in it to select the right tuples. Neo4j can
 * keepo track of the different entities using labels, therfore the discriminator value becomes another metdata
 * identifier.
 * <p>
 * This class is used to select the right queries to use with the dialect. {@link EntityKeyMetadata} is not enough
 * because it is the same for all the entities in the hierarchy.
 *
 * @author Davide D'Alto
 */
public class EntityKeyMetadataWithDiscriminator implements EntityKeyMetadata {

	private final EntityKeyMetadata entityKeyMetadata;
	private final Object discriminatorValue;

	public EntityKeyMetadataWithDiscriminator(EntityKeyMetadata entityKeyMetadata, Object discriminatorValue) {
		this.entityKeyMetadata = entityKeyMetadata;
		this.discriminatorValue = discriminatorValue;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public String getTable() {
		return entityKeyMetadata.getTable();
	}

	@Override
	public String[] getColumnNames() {
		return entityKeyMetadata.getColumnNames();
	}

	@Override
	public boolean isKeyColumn(String columnName) {
		return entityKeyMetadata.isKeyColumn( columnName );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( discriminatorValue == null ) ? 0 : discriminatorValue.hashCode() );
		result = prime * result + ( ( entityKeyMetadata == null ) ? 0 : entityKeyMetadata.hashCode() );
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
		EntityKeyMetadataWithDiscriminator other = (EntityKeyMetadataWithDiscriminator) obj;
		if ( discriminatorValue == null ) {
			if ( other.discriminatorValue != null ) {
				return false;
			}
		}
		else if ( !discriminatorValue.equals( other.discriminatorValue ) ) {
			return false;
		}
		if ( entityKeyMetadata == null ) {
			if ( other.entityKeyMetadata != null ) {
				return false;
			}
		}
		else if ( !entityKeyMetadata.equals( other.entityKeyMetadata ) ) {
			return false;
		}
		return true;
	}
}
