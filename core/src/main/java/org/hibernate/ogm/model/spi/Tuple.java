/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import static org.hibernate.ogm.model.spi.TupleOperationType.PUT;
import static org.hibernate.ogm.model.spi.TupleOperationType.PUT_NULL;
import static org.hibernate.ogm.model.spi.TupleOperationType.REMOVE;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.impl.SetFromCollection;

/**
 * Represents a Tuple (think of it as a row)
 *
 * A tuple accepts a TupleShapshot which is a read-only state
 * of the tuple at creation time.
 *
 * A tuple collects changes applied to it. These changes are represented by a
 * list of TupleOperation. It is intended that GridDialects retrieve to these actions and
 * reproduce them to the datastore. The list of changes is computed based off the snapshot.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero  &lt;sanne@hibernate.org&gt;
 */
public class Tuple {

	private final TupleSnapshot snapshot;
	private Map<String, TupleOperation> currentState = null; //lazy initialize the Map as it costs quite some memory

	public Tuple() {
		this.snapshot = EmptyTupleSnapshot.INSTANCE;
	}

	public Tuple(TupleSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public Object get(String column) {
		if ( currentState == null ) {
			return snapshot.get( column );
		}
		TupleOperation result = currentState.get( column );
		if ( result == null ) {
			return snapshot.get( column );
		}
		else if ( result.getType() == PUT_NULL || result.getType() == REMOVE ) {
			return null;
		}
		else {
			return result.getValue();
		}
	}

	public void put(String column, Object value) {
		if ( currentState == null ) {
			currentState = new HashMap<String, TupleOperation>();
		}
		if ( value == null ) {
			currentState.put( column, new TupleOperation( column, null, PUT_NULL ) );
		}
		else {
			currentState.put( column, new TupleOperation( column, value, PUT ) );
		}
	}

	public void remove(String column) {
		if ( currentState == null ) {
			currentState = new HashMap<String, TupleOperation>();
		}
		currentState.put( column, new TupleOperation( column, null, REMOVE ) );
	}

	/**
	 * Return the list of actions on the tuple.
	 * Inherently deduplicated operations
	 */
	public Set<TupleOperation> getOperations() {
		if ( currentState == null ) {
			return Collections.emptySet();
		}
		else {
			return new SetFromCollection<TupleOperation>( currentState.values() );
		}
	}

	public TupleSnapshot getSnapshot() {
		return snapshot;
	}

	public Set<String> getColumnNames() {
		if ( currentState == null ) {
			return snapshot.getColumnNames();
		}
		Set<String> columnNames = new HashSet<String>( snapshot.getColumnNames() );
		for ( TupleOperation op : currentState.values() ) {
			switch ( op.getType() ) {
				case PUT :
				case PUT_NULL :
					columnNames.add( op.getColumn() );
					break;
				case REMOVE:
					columnNames.remove( op.getColumn() );
					break;
			}
		}
		return columnNames;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( "Tuple[");
		int i = 0;
		for ( String column : getColumnNames() ) {
			sb.append( column ).append( "=" ).append( get( column ) );
			i++;
			if ( i < getColumnNames().size() ) {
				sb.append( ", " );
			}
		}

		sb.append( "]" );
		return sb.toString();
	}
}
