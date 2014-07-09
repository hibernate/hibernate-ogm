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
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NewsID newsID = (NewsID) o;

		if ( author != null ? !author.equals( newsID.author ) : newsID.author != null ) {
			return false;
		}
		if ( title != null ? !title.equals( newsID.title ) : newsID.title != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = title != null ? title.hashCode() : 0;
		result = 31 * result + ( author != null ? author.hashCode() : 0 );
		return result;
	}
}
