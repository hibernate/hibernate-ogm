/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Gunnar Morling
 */
@Entity
public class MakeupArtist {

	@Id
	private String id;
	private String favoriteStyle;

	public MakeupArtist() {
	}

	public MakeupArtist(String id, String favoriteStyle) {
		this.id = id;
		this.favoriteStyle = favoriteStyle;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFavoriteStyle() {
		return favoriteStyle;
	}

	public void setFavoriteStyle(String favoriteStyle) {
		this.favoriteStyle = favoriteStyle;
	}
}
