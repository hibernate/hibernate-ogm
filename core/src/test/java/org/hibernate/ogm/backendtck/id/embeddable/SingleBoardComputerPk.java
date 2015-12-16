/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.embeddable;

import java.io.Serializable;

import javax.persistence.Embeddable;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Gunnar Morling
 */
@Embeddable
public class SingleBoardComputerPk implements Serializable {

	private String id;

	SingleBoardComputerPk() {
	}

	public SingleBoardComputerPk(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
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
		SingleBoardComputerPk other = (SingleBoardComputerPk) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		}
		else if ( !id.equals( other.id ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SingleBoardComputerPk [id=" + id + "]";
	}

	public static class SingleBoardComputerPkFieldBridge implements TwoWayFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			if ( value == null ) {
				return;
			}

			luceneOptions.addFieldToDocument( name, ( (SingleBoardComputerPk) value ).id, document );
		}

		@Override
		public Object get(String name, Document document) {
			return new SingleBoardComputerPk( document.get( name ) );
		}

		@Override
		public String objectToString(Object object) {
			return ( (SingleBoardComputerPk) object ).id;
		}
	}
}
