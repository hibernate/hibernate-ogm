/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.gen;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
@GenericGenerator(name = "customGen", strategy = "org.hibernate.ogm.backendtck.id.gen.CustomIdGenerator")
public class CustomIdGeneratorEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "customGen")
	String id;

	CustomIdGeneratorEntity(String id) {
		this.id = id;
	}

	public CustomIdGeneratorEntity() {
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		CustomIdGeneratorEntity that = (CustomIdGeneratorEntity) o;
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}
}
