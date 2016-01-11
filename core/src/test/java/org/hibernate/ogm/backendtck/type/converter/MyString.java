/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.converter;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class MyString {

	private final String string;

	public MyString(String string) {
		// note that the string is explicitly not lower case sanitized
		// we let the converter do this job to see if it is called
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( string == null ) ? 0 : string.hashCode() );
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
		MyString other = (MyString) obj;
		if ( string == null ) {
			if ( other.string != null ) {
				return false;
			}
		}
		else if ( !string.equals( other.string ) ) {
			return false;
		}
		return true;
	}
}
