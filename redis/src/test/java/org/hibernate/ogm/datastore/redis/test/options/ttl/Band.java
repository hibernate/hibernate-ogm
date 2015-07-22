/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.options.ttl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.ogm.datastore.redis.options.TTL;

/**
 * @author Gunnar Morling
 */
@Entity
public class Band {

	@Id
	private String id;

	private String name;

	@OneToMany
	@TTL(60)
	private List<Song> songs;

	Band() {
	}


	public Band(String id, String name, Song... songs) {
		this.id = id;
		this.name = name;
		this.songs = new ArrayList<Song>(
				songs != null ?
						Arrays.asList( songs ) :
						Collections.<Song>emptyList()
		);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSongs(List<Song> songs) {
		this.songs = songs;
	}

	public List<Song> getSongs() {
		return songs;
	}
}
