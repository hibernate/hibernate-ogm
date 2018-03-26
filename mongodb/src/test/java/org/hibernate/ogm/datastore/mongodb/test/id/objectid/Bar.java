/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.bson.types.ObjectId;

/**
 * @author Gunnar Morling
 */
@Entity
public class Bar {

	private ObjectId id;
	private String name;
	private MusicGenre musicGenre;
	private Set<DoorMan> doorMen = new HashSet<DoorMan>();

	Bar() {
	}

	Bar(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	public MusicGenre getMusicGenre() {
		return musicGenre;
	}

	public void setMusicGenre(MusicGenre musicGenre) {
		this.musicGenre = musicGenre;
	}

	@OneToMany(cascade = CascadeType.ALL)
	public Set<DoorMan> getDoorMen() {
		return doorMen;
	}

	public void setDoorMen(Set<DoorMan> doorMen) {
		this.doorMen = doorMen;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( doorMen == null ) ? 0 : doorMen.hashCode() );
		result = prime * result + ( ( musicGenre == null ) ? 0 : musicGenre.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
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
		Bar other = (Bar) obj;
		return Objects.equals( doorMen, other.doorMen )
				&& Objects.equals( musicGenre, other.musicGenre )
				&& Objects.equals( name, other.name );
	}
}
