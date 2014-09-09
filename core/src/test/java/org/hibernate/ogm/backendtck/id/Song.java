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
public class Song {
	static final transient int INITIAL_VALUE = 2;
	private Long id;
	private String title;
	private String singer;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "songSequenceGenerator")
	@SequenceGenerator(name = "songSequenceGenerator",
			sequenceName = "song_sequence_name",
			initialValue = INITIAL_VALUE,
			allocationSize = 10)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSinger() {
		return singer;
	}

	public void setSinger(String singer) {
		this.singer = singer;
	}
}
