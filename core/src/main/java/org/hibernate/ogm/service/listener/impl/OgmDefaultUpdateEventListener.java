/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.listener.impl;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.EntityIdentityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.internal.DefaultUpdateEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.entityentry.impl.OgmEntityEntryState;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * A almost 1:1 copy of ORM's {@link DefaultPersistEventListener}. Used as a temp. work-around until HHH-9451 is
 * resolved. The only difference to the original class is that the extra entity state of temporary entity entries is
 * propagated to the final entries in
 * {@link #performSaveOrReplicate(Object, EntityKey, EntityPersister, boolean, Object, EventSource, boolean)}.
 *
 * @author Gavin King
 * @author Gunnar Morling
 */
public class OgmDefaultUpdateEventListener extends DefaultUpdateEventListener {

	/**
	 * Performs all the actual work needed to save an entity (well to get the save moved to
	 * the execution queue).
	 *
	 * @param entity The entity to be saved
	 * @param key The id to be used for saving the entity (or null, in the case of identity columns)
	 * @param persister The entity's persister instance.
	 * @param useIdentityColumn Should an identity column be used for id generation?
	 * @param anything Generally cascade-specific information.
	 * @param source The session which is the source of the current event.
	 * @param requiresImmediateIdAccess Is access to the identifier required immediately
	 * after the completion of the save?  persist(), for example, does not require this...
	 *
	 * @return The id used to save the entity; may be null depending on the
	 *         type of id generator used and the requiresImmediateIdAccess value
	 */
	@Override
	protected Serializable performSaveOrReplicate(
			Object entity,
			EntityKey key,
			EntityPersister persister,
			boolean useIdentityColumn,
			Object anything,
			EventSource source,
			boolean requiresImmediateIdAccess) {

		Serializable id = key == null ? null : key.getIdentifier();

		boolean inTxn = source.getTransactionCoordinator().isTransactionInProgress();
		boolean shouldDelayIdentityInserts = !inTxn && !requiresImmediateIdAccess;

		// Put a placeholder in entries, so we don't recurse back and try to save() the
		// same object again. QUESTION: should this be done before onSave() is called?
		// likewise, should it be done before onUpdate()?
		EntityEntry original = source.getPersistenceContext().addEntry(
				entity,
				Status.SAVING,
				null,
				null,
				id,
				null,
				LockMode.WRITE,
				useIdentityColumn,
				persister,
				false,
				false
		);

		cascadeBeforeSave( source, persister, entity, anything );

		Object[] values = persister.getPropertyValuesToInsert( entity, getMergeMap( anything ), source );
		Type[] types = persister.getPropertyTypes();

		boolean substitute = substituteValuesIfNecessary( entity, id, values, persister, source );

		if ( persister.hasCollections() ) {
			substitute = substitute || visitCollectionsBeforeSave( entity, id, values, types, source );
		}

		if ( substitute ) {
			persister.setPropertyValues( entity, values );
		}

		TypeHelper.deepCopy(
				values,
				types,
				persister.getPropertyUpdateability(),
				values,
				source
		);

		AbstractEntityInsertAction insert = addInsertAction(
				values, id, entity, persister, useIdentityColumn, source, shouldDelayIdentityInserts
		);

		// postpone initializing id in case the insert has non-nullable transient dependencies
		// that are not resolved until cascadeAfterSave() is executed
		cascadeAfterSave( source, persister, entity, anything );
		if ( useIdentityColumn && insert.isEarlyInsert() ) {
			if ( !EntityIdentityInsertAction.class.isInstance( insert ) ) {
				throw new IllegalStateException(
						"Insert should be using an identity column, but action is of unexpected type: " +
								insert.getClass().getName()
				);
			}
			id = ((EntityIdentityInsertAction) insert).getGeneratedId();

			insert.handleNaturalIdPostSaveNotifications( id );
		}

		markInterceptorDirty( entity, persister, source );

		EntityEntry newEntry = source.getPersistenceContext().getEntry( entity );

		if ( newEntry != original ) {
			OgmEntityEntryState ogmEntityState = newEntry.getExtraState( OgmEntityEntryState.class );
			if ( ogmEntityState == null ) {
				newEntry.addExtraState( original.getExtraState( OgmEntityEntryState.class ) );
			}
		}

		return id;
	}

	private AbstractEntityInsertAction addInsertAction(
			Object[] values,
			Serializable id,
			Object entity,
			EntityPersister persister,
			boolean useIdentityColumn,
			EventSource source,
			boolean shouldDelayIdentityInserts) {
		if ( useIdentityColumn ) {
			EntityIdentityInsertAction insert = new EntityIdentityInsertAction(
					values, entity, persister, isVersionIncrementDisabled(), source, shouldDelayIdentityInserts
			);
			source.getActionQueue().addAction( insert );
			return insert;
		}
		else {
			Object version = Versioning.getVersion( values, persister );
			EntityInsertAction insert = new EntityInsertAction(
					id, values, entity, version, persister, isVersionIncrementDisabled(), source
			);
			source.getActionQueue().addAction( insert );
			return insert;
		}
	}

	private void markInterceptorDirty(Object entity, EntityPersister persister, EventSource source) {
		if ( persister.getInstrumentationMetadata().isInstrumented() ) {
			FieldInterceptor interceptor = persister.getInstrumentationMetadata().injectInterceptor(
					entity,
					persister.getEntityName(),
					null,
					source
			);
			interceptor.dirty();
		}
	}
}
