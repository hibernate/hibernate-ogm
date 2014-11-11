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
public class MakeupArtistWithCompositeKey {

	@Id
	private MakeUpArtistId id;
	private String favoriteStyle;

	public MakeupArtistWithCompositeKey() {
	}

	public MakeupArtistWithCompositeKey(MakeUpArtistId id, String favoriteStyle) {
		this.id = id;
		this.favoriteStyle = favoriteStyle;
	}

	public MakeUpArtistId getId() {
		return id;
	}

	public void setId(MakeUpArtistId id) {
		this.id = id;
	}

	public String getFavoriteStyle() {
		return favoriteStyle;
	}

	public void setFavoriteStyle(String favoriteStyle) {
		this.favoriteStyle = favoriteStyle;
	}
}
