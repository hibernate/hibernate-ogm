/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.mapping;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Owns a {@link Byte} field
 *
 * @author Fabio Massimo Ercoli
 */
@Entity
public class ByteEntity {

	@Id
	private Integer id;

	private Byte counter;

	public ByteEntity() {
	}

	public ByteEntity(Integer id, Byte counter) {
		this.id = id;
		this.counter = counter;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Byte getCounter() {
		return counter;
	}

	public void setCounter(Byte counter) {
		this.counter = counter;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ByteEntity that = (ByteEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( counter, that.counter );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, counter );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "ByteEntity{" );
		sb.append( "id=" ).append( id );
		sb.append( ", counter=" ).append( counter );
		sb.append( '}' );
		return sb.toString();
	}
}
