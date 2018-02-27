/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.entityentry.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Entity-dependent state specific to Hibernate OGM.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class OgmEntityEntryState implements EntityEntryExtraState {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private EntityEntryExtraState next;
	private final TuplePointer tuplePointer = new TuplePointer();
	private Map<String, Association> associations;

	/**
	 * Return the stable pointer to the {@link Tuple} representing the given entity, as loaded from the datastore.
	 *
	 * @return the pointer to the tuple representing the given entity
	 */
	public TuplePointer getTuplePointer() {
		return tuplePointer;
	}

	/**
	 * Return the association as cached in the entry state.
	 *
	 * @param collectionRole the role of the association
	 * @return the cached association
	 */
	public Association getAssociation(String collectionRole) {
		if ( associations == null ) {
			return null;
		}
		return associations.get( collectionRole );
	}

	/**
	 * Indicates if the entry state contains information about the given association.
	 *
	 * @param collectionRole the role of the association
	 * @return true if the entry state contains information about the given association
	 */
	public boolean hasAssociation(String collectionRole) {
		if ( associations == null ) {
			return false;
		}
		return associations.containsKey( collectionRole );
	}

	/**
	 * Set the association in the entry state.
	 *
	 * @param collectionRole the role of the association
	 * @param association the association
	 */
	public void setAssociation(String collectionRole, Association association) {
		if ( associations == null ) {
			associations = new HashMap<>();
		}
		associations.put( collectionRole, association );
	}

	public static OgmEntityEntryState getStateFor(SharedSessionContractImplementor session, Object object) {
		EntityEntry entityEntry = session.getPersistenceContext().getEntry( object );
		if ( entityEntry == null ) {
			throw log.cannotFindEntityEntryForEntity( object );
		}

		OgmEntityEntryState ogmEntityState = entityEntry.getExtraState( OgmEntityEntryState.class );
		if ( ogmEntityState == null ) {
			ogmEntityState = new OgmEntityEntryState();
			entityEntry.addExtraState( ogmEntityState );
		}

		return ogmEntityState;
	}

	// state chain management ops below

	@Override
	public void addExtraState(EntityEntryExtraState extraState) {
		if ( next == null ) {
			next = extraState;
		}
		else {
			next.addExtraState( extraState );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType) {
		if ( next == null ) {
			return null;
		}
		if ( extraStateType.isAssignableFrom( next.getClass() ) ) {
			return (T) next;
		}
		else {
			return next.getExtraState( extraStateType );
		}
	}
}
