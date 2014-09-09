/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.ogm.id.impl.OgmSequenceGenerator;

/**
 * Test entity for {@link OgmSequenceGenerator}.
 *
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 */
@Entity
public class Actor {
	static final transient int INITIAL_VALUE = 1;
	private Long id;
	private String name;
	private String totalMovies;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actorSequenceGenerator")
	@SequenceGenerator(name = "actorSequenceGenerator",
			sequenceName = "actor_sequence_name",
			initialValue = INITIAL_VALUE,
			allocationSize = 10)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBestMovieTitle() {
		return totalMovies;
	}

	public void setBestMovieTitle(String bestMovieTitle) {
		this.totalMovies = bestMovieTitle;
	}
}
