/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneType extends GridTypeDelegatingToCoreType implements GridType {
	private final TypeTranslator typeTranslator;
	private final org.hibernate.type.ManyToOneType delegate;

	public ManyToOneType(org.hibernate.type.ManyToOneType type, TypeTranslator typeTranslator) {
		super( type );
		this.delegate = type;
		this.typeTranslator = typeTranslator;
	}

	@Override
	public Object nullSafeGet(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return resolve( hydrate( rs, names, session, owner ), session, owner );
	}

	@Override
	public Object nullSafeGet(Tuple rs, String name, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		GridType idGridType = getIdGridType( session );
		idGridType.nullSafeSet( resultset, getIdentifier( value, session ), names, settable, session );
	}

	private GridType getIdGridType(SessionImplementor session) {
		final Type idType = delegate.getIdentifierOrUniqueKeyType( session.getFactory() );
		GridType idGridType = typeTranslator.getType( idType );
		return idGridType;
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, SessionImplementor session)
			throws HibernateException {
		GridType idGridType = getIdGridType( session );
		idGridType.nullSafeSet( resultset, getIdentifier( value, session ), names, session );
	}

	@Override
	public Object hydrate(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		// return the (fully resolved) identifier value, but do not resolve
		// to the actual referenced entity instance
		// NOTE: the owner of the association is not really the owner of the id!
		Serializable id = (Serializable) getIdGridType( session ).nullSafeGet( rs, names, session, null );
		scheduleBatchLoadIfNeeded( id, session );
		return id;
	}

	/**
	 * Register the entity as batch loadable, if enabled
	 *
	 * Copied from ManyToOne#scheduleBatchLoadIfNeeded
	 */
	private void scheduleBatchLoadIfNeeded(Serializable id, SessionImplementor session) throws MappingException {
		//cannot batch fetch by unique key (property-ref associations)
		//FIXME support non-pk unique id
		if ( //uniqueKeyPropertyName == null &&
				id != null ) {
			EntityPersister persister = session.getFactory().getEntityPersister( delegate.getAssociatedEntityName() );
			EntityKey entityKey = session.generateEntityKey( id, persister );
			if ( !session.getPersistenceContext().containsEntity( entityKey ) ) {
				session.getPersistenceContext().getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
			}
		}
	}

	//copied from org.hibernate.type.ManyToOne#getIdentifier()
	protected final Object getIdentifier(Object value, SessionImplementor session) throws HibernateException {
		//isNotEmbedded copied from EntityType#isNotEmbedded
		boolean isNotEmbedded = isNotEmbedded( session );
		if ( isNotEmbedded ) {
			return value;
		}

		final String associatedEntityName = delegate.getAssociatedEntityName();
		if ( delegate.isReferenceToPrimaryKey() ) {
			return ForeignKeys.getEntityIdentifierIfNotUnsaved( associatedEntityName, value, session ); //tolerates nulls
		}
		else if ( value == null ) {
			return null;
		}
		else {
			//FIXME get access to uniqueKeyPropertyName from somewhere :/
//			EntityPersister entityPersister = session.getFactory().getEntityPersister( associatedEntityName );
//			Object propertyValue = entityPersister.getPropertyValue( value, uniqueKeyPropertyName, session.getEntityMode() );
//			// We now have the value of the property-ref we reference.  However,
//			// we need to dig a little deeper, as that property might also be
//			// an entity type, in which case we need to resolve its identitifier
//			Type type = entityPersister.getPropertyType( uniqueKeyPropertyName );
//			if ( type.isEntityType() ) {
//				propertyValue = ( ( EntityType ) type ).getIdentifier( propertyValue, session );
//			}
//
//			return propertyValue;
			throw new NotYetImplementedException( "@ManyToOne using a non-pk unique key not yet supported by OGM");
		}
	}

	protected boolean isNotEmbedded(SessionImplementor session) {
		return false;
	}
}
