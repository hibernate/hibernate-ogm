/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations.ordercolumn;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * @author Gunnar Morling
 */
@Entity
public class Writer {
	private String id;
	private String name;
	private List<Episode> episodes = new ArrayList<Episode>();

	Writer() {
	}

	public Writer(String id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
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

	@ManyToMany(mappedBy = "authors")
//	@JoinColumn(name = "tv_show_id")
//	@Cascade({ CascadeType.PERSIST, CascadeType.SAVE_UPDATE, CascadeType.DELETE })
	public List<Episode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(List<Episode> episodes) {
		this.episodes = episodes;
	}
}
