/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import javax.persistence.Embeddable;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Store;

@Embeddable
public class Ending {

	// Analyze.NO for filtering in query
	// Store.YES for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private String text;

	// Analyze.NO for filtering in query
	// Store.YES for projection in query
	@Field(store = Store.YES, analyze = Analyze.NO)
	private Integer score;

	public Ending() {
	}

	public Ending(String text, Integer score) {
		this.text = text;
		this.score = score;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( score == null ) ? 0 : score.hashCode() );
		result = prime * result + ( ( text == null ) ? 0 : text.hashCode() );
		return result;
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
		Ending other = (Ending) obj;
		if ( score == null ) {
			if ( other.score != null ) {
				return false;
			}
		}
		else if ( !score.equals( other.score ) ) {
			return false;
		}
		if ( text == null ) {
			if ( other.text != null ) {
				return false;
			}
		}
		else if ( !text.equals( other.text ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "Ending [text=" );
		builder.append( text );
		builder.append( ", score=" );
		builder.append( score );
		builder.append( "]" );
		return builder.toString();
	}
}
