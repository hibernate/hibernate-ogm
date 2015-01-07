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
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

/**
 * @author Gunnar Morling
 */
@Entity
public class TvShow {
	private String id;
	private String name;
	private List<Episode> episodes = new ArrayList<Episode>();

	TvShow() {
	}

	public TvShow(String id, String name) {
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

	@OneToMany
	@JoinColumn(name = "tv_show_id")
	@OrderColumn(name = "idx")
	@Cascade({ CascadeType.PERSIST, CascadeType.SAVE_UPDATE, CascadeType.DELETE })
	public List<Episode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(List<Episode> episodes) {
		this.episodes = episodes;
	}
}
