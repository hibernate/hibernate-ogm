package org.hibernate.ogm.grid;

import java.io.Serializable;

/**
 * Entity key
 *
 * @author Emmanuel Bernard
 */
public class Key {
	private final Class<?> type;
	private final Serializable id;

	public Key(Class<?> type, Serializable id) {
		this.type = type;
		this.id = id;
	}

	public Class<?> getType() {
		return type;
	}

	public Serializable getId() {
		return id;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Key" );
		sb.append( "{type=" ).append( type );
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
		if ( !type.equals( key.type ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + ( id != null ? id.hashCode() : 0 );
		return result;
	}
}
