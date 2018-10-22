/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.elementcollection;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Programmer {

	@Id
	private String nick;

	public Programmer() {
	}

	public Programmer(String nick) {
		this.nick = nick;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Programmer that = (Programmer) o;
		return Objects.equals( nick, that.nick );
	}

	@Override
	public int hashCode() {
		return Objects.hash( nick );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "Programmer{" );
		sb.append( "nick='" ).append( nick ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
