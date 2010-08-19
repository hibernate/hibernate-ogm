package org.hibernate.ogm.grid;

import java.io.Serializable;

/**
 * Entity key
 *
 * @author Emmanuel Bernard
 */
public class Key {
	private final String table;
	private final Serializable id;

	public Key(String table, Serializable id) {
		this.table = table;
		this.id = id;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Key" );
		sb.append( "{table=" ).append( table );
		sb.append( ", id=" ).append( id );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Key key = ( Key ) o;

		if ( id != null ? !id.equals( key.id ) : key.id != null ) {
			return false;
		}
		if ( !table.equals( key.table ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = table.hashCode();
		result = 31 * result + ( id != null ? id.hashCode() : 0 );
		return result;
	}
}
