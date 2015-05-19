/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.parameters;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;

/**
 * @author Gunnar Morling
 */
@Entity
@Indexed
public class Movie {

	@Id
	private String id;

	@Enumerated(EnumType.ORDINAL)
	@Field(analyze = Analyze.NO, store = Store.YES)
	private Genre genre;

	private String title;

	@Field(analyze = Analyze.NO, store = Store.YES)
	@Type(type = "yes_no")
	private boolean suitableForKids;

	@Temporal(TemporalType.DATE)
	@Field(analyze = Analyze.NO, store = Store.YES)
	@DateBridge(encoding = EncodingType.STRING, resolution = Resolution.DAY)
	private Date releaseDate;

	@Field(analyze = Analyze.NO, store = Store.YES)
	private byte viewerRating;

	Movie() {
	}

	public Movie(String id) {
		this.id = id;
	}

	public Movie(String id, Genre genre, String title, boolean suitableForKids, Date releaseDate, byte viewerRating) {
		this.id = id;
		this.genre = genre;
		this.title = title;
		this.suitableForKids = suitableForKids;
		this.releaseDate = releaseDate;
		this.viewerRating = viewerRating;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public boolean isSuitableForKids() {
		return suitableForKids;
	}

	public void setSuitableForKids(boolean suitableForKids) {
		this.suitableForKids = suitableForKids;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public byte getViewerRating() {
		return viewerRating;
	}

	public void setViewerRating(byte viewerRating) {
		this.viewerRating = viewerRating;
	}
}
