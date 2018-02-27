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
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneType extends EntityType {

	public ManyToOneType(org.hibernate.type.ManyToOneType type, TypeTranslator typeTranslator) {
		super( type, typeTranslator );
	}

	@Override
	public Object nullSafeGet(Tuple rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return resolve( hydrate( rs, names, session, owner ), session, owner );
	}

	@Override
	public Object nullSafeGet(Tuple rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException {
		GridType idGridType = getIdGridType( session.getFactory() );
		idGridType.nullSafeSet( resultset, getIdentifier( value, session ), names, settable, session );
	}

	private GridType getIdGridType(SessionFactoryImplementor sessionFactory) {
		final Type idType = delegate.getIdentifierOrUniqueKeyType( sessionFactory );
		GridType idGridType = typeTranslator.getType( idType );
		return idGridType;
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, SharedSessionContractImplementor session)
			throws HibernateException {
		GridType idGridType = getIdGridType( session.getFactory() );
		idGridType.nullSafeSet( resultset, getIdentifier( value, session ), names, session );
	}

	@Override
	public Object hydrate(Tuple rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		// return the (fully resolved) identifier value, but do not resolve
		// to the actual referenced entity instance
		// NOTE: the owner of the association is not really the owner of the id!
		Serializable id = (Serializable) getIdGridType( session.getFactory() ).nullSafeGet( rs, names, session, null );
		scheduleBatchLoadIfNeeded( id, session );
		return id;
	}

	/**
	 * Register the entity as batch loadable, if enabled
	 *
	 * Copied from {@link org.hibernate.type.ManyToOneType#scheduleBatchLoadIfNeeded}
	 */
	private void scheduleBatchLoadIfNeeded(Serializable id, SharedSessionContractImplementor session) throws MappingException {
		//cannot batch fetch by unique key (property-ref associations)
		if ( StringHelper.isEmpty( delegate.getRHSUniqueKeyPropertyName() ) && id != null ) {
			EntityPersister persister = session.getFactory().getMetamodel().entityPersister( delegate.getAssociatedEntityName() );
			EntityKey entityKey = session.generateEntityKey( id, persister );
			if ( !session.getPersistenceContext().containsEntity( entityKey ) ) {
				session.getPersistenceContext().getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
			}
		}
	}

}
