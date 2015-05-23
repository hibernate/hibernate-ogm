/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import java.io.Serializable;

import javax.persistence.Embeddable;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
@Embeddable
public class NewsID implements Serializable {

	private String title;
	private String author;

	public NewsID() {
		super();
	}

	public NewsID(String title, String author) {
		this.title = title;
		this.author = author;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
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
		NewsID other = (NewsID) obj;
		if ( author == null ) {
			if ( other.author != null ) {
				return false;
			}
		}
		else if ( !author.equals( other.author ) ) {
			return false;
		}
		if ( title == null ) {
			if ( other.title != null ) {
				return false;
			}
		}
		else if ( !title.equals( other.title ) ) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( author == null ) ? 0 : author.hashCode() );
		result = prime * result + ( ( title == null ) ? 0 : title.hashCode() );
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "NewsID [title=" );
		builder.append( title );
		builder.append( ", author=" );
		builder.append( author );
		builder.append( "]" );
		return builder.toString();
	}
}
