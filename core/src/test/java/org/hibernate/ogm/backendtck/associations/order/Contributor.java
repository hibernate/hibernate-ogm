/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.order;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Contributor {

	@Id
	private String nick;
	private Integer commits = 0;

	public Contributor() {
	}

	public Contributor(String nick, Integer commits) {
		this.nick = nick;
		this.commits = commits;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public Integer getCommits() {
		return commits;
	}

	public void setCommits(Integer commits) {
		this.commits = commits;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Contributor that = (Contributor) o;
		return Objects.equals( nick, that.nick ) &&
				Objects.equals( commits, that.commits );
	}

	@Override
	public int hashCode() {
		return Objects.hash( nick, commits );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "Contributor{" );
		sb.append( "nick='" ).append( nick ).append( '\'' );
		sb.append( ", commits=" ).append( commits );
		sb.append( '}' );
		return sb.toString();
	}
}
