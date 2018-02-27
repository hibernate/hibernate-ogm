/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.HibernateException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 * @author Guillaume Smet
 */
public abstract class EntityType extends GridTypeDelegatingToCoreType {
	protected final TypeTranslator typeTranslator;
	protected final org.hibernate.type.EntityType delegate;

	public EntityType(org.hibernate.type.EntityType type, TypeTranslator typeTranslator) {
		super( type );
		this.delegate = type;
		this.typeTranslator = typeTranslator;
	}

	@Override
	public Object convertToBackendType(Object value, SessionFactoryImplementor sessionFactory) {
		return getIdentifier( value, sessionFactory );
	}

	//copied from org.hibernate.type.ManyToOneType#getIdentifier()
	protected final Object getIdentifier(Object value, SharedSessionContractImplementor session) throws HibernateException {
		if ( value == null ) {
			return null;
		}

		//isNotEmbedded copied from EntityType#isNotEmbedded
		boolean isNotEmbedded = isNotEmbedded( session );
		if ( isNotEmbedded ) {
			return value;
		}

		final String associatedEntityName = delegate.getAssociatedEntityName();
		final String uniqueKeyPropertyName = delegate.getRHSUniqueKeyPropertyName();

		if ( StringHelper.isEmpty( uniqueKeyPropertyName ) ) {
			return ForeignKeys.getEntityIdentifierIfNotUnsaved( associatedEntityName, value, session ); //tolerates nulls
		}
		else {
			final EntityPersister persister = session.getFactory().getMetamodel().entityPersister( delegate.getAssociatedEntityName() );

			Object propertyValue = persister.getPropertyValue( value, uniqueKeyPropertyName );
			// We now have the value of the property-ref we reference.  However,
			// we need to dig a little deeper, as that property might also be
			// an entity type, in which case we need to resolve its identifier
			Type type = persister.getPropertyType( uniqueKeyPropertyName );
			GridType gridType = typeTranslator.getType( type );
			if ( gridType.isEntityType() ) {
				propertyValue = ( (EntityType) gridType ).getIdentifier( propertyValue, session );
			}

			return propertyValue;
		}
	}

	//inspired by org.hibernate.type.ManyToOneType#getIdentifier()
	protected final Object getIdentifier(Object value, SessionFactoryImplementor sessionFactory) throws HibernateException {
		if ( value == null ) {
			return null;
		}

		final EntityPersister persister = sessionFactory.getMetamodel().entityPersister( delegate.getAssociatedEntityName() );
		final String uniqueKeyPropertyName = delegate.getRHSUniqueKeyPropertyName();

		if ( StringHelper.isEmpty( uniqueKeyPropertyName ) ) {
			return persister.getIdentifier( value, null );
		}
		else {
			Object propertyValue = persister.getPropertyValue( value, uniqueKeyPropertyName );
			// We now have the value of the property-ref we reference.  However,
			// we need to dig a little deeper, as that property might also be
			// an entity type, in which case we need to resolve its identifier
			Type type = persister.getPropertyType( uniqueKeyPropertyName );
			GridType gridType = typeTranslator.getType( type );
			if ( gridType.isEntityType() ) {
				propertyValue = getIdentifier( propertyValue, sessionFactory );
			}

			return propertyValue;
		}
	}

	protected boolean isNotEmbedded(SharedSessionContractImplementor session) {
		return false;
	}

}
