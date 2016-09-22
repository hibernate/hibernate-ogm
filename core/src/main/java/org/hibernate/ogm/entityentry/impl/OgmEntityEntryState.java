/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.entityentry.impl;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Entity-dependent state specific to Hibernate OGM.
 *
 * @author Gunnar Morling
 */
public class OgmEntityEntryState implements EntityEntryExtraState {

	private static final Log log = LoggerFactory.make();

	private EntityEntryExtraState next;
	private Tuple tuple;

	/**
	 * The {@link Tuple} representing the given entity, as loaded from the datastore. May be {@code null} in case the
	 * loading code failed to set it.
	 *
	 * @return the tuple representing the given entity
	 */
	public Tuple getTuple() {
		return tuple;
	}

	public void setTuple(Tuple tuple) {
		this.tuple = tuple;
	}

	public static OgmEntityEntryState getStateFor(SessionImplementor session, Object object) {
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
