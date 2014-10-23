/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Provides utility methods for dealing with bi-directional associations.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class BiDirectionalAssociationHelper {

	private BiDirectionalAssociationHelper() {
	}

	/**
	 * Returns the meta-data for the inverse side of the association represented by the given property on the given
	 * persister in case it represents the main side of a bi-directional one-to-many or many-to-many association.
	 *
	 * @param mainSidePersister persister of the entity hosting the property of interest
	 * @param propertyIndex index of the property of interest
	 * @return the meta-data of the inverse side of the specified association or {@code null} if no such meta-data
	 * exists
	 */
	public static AssociationKeyMetadata getInverseAssociationKeyMetadata(OgmEntityPersister mainSidePersister, int propertyIndex) {
		Type propertyType = mainSidePersister.getPropertyTypes()[propertyIndex];
		SessionFactoryImplementor factory = mainSidePersister.getFactory();

		// property represents no association, so no inverse meta-data can exist
		if ( !propertyType.isAssociationType() ) {
			return null;
		}

		Joinable mainSideJoinable = ( (AssociationType) propertyType ).getAssociatedJoinable( factory );
		OgmEntityPersister inverseSidePersister = null;

		// to-many association
		if ( mainSideJoinable.isCollection() ) {
			inverseSidePersister = (OgmEntityPersister) ( (OgmCollectionPersister) mainSideJoinable ).getElementPersister();
		}
		// to-one
		else {
			inverseSidePersister = (OgmEntityPersister) mainSideJoinable;
			mainSideJoinable = mainSidePersister;
		}

		String mainSideProperty = mainSidePersister.getPropertyNames()[propertyIndex];

		// property is a one-to-one association (a many-to-one cannot be on the inverse side) -> get the meta-data
		// straight from the main-side persister
		AssociationKeyMetadata inverseOneToOneMetadata = mainSidePersister.getInverseOneToOneAssociationKeyMetadata( mainSideProperty );
		if ( inverseOneToOneMetadata != null ) {
			return inverseOneToOneMetadata;
		}

		// process properties of inverse side and try to find association back to main side
		for ( String candidateProperty : inverseSidePersister.getPropertyNames() ) {
			Type type = inverseSidePersister.getPropertyType( candidateProperty );

			// candidate is a *-to-many association
			if ( type.isCollectionType() ) {
				OgmCollectionPersister inverseCollectionPersister = getPersister( factory, (CollectionType) type );
				if ( isCollectionMatching( mainSideJoinable, inverseCollectionPersister ) ) {
					return inverseCollectionPersister.getAssociationKeyMetadata();
				}
			}
		}

		return null;
	}

	/**
	 * Returns the given collection persister for the inverse side in case the given persister represents the main side
	 * of a bi-directional many-to-many association.
	 *
	 * @param mainSidePersister the collection persister on the main side of a bi-directional many-to-many association
	 * @return the collection persister for the inverse side of the given persister or {@code null} in case it
	 * represents the inverse side itself or the association is uni-directional
	 */
	public static OgmCollectionPersister getInverseCollectionPersister(OgmCollectionPersister mainSidePersister) {
		if ( mainSidePersister.isInverse() || !mainSidePersister.isManyToMany() || !mainSidePersister.getElementType().isEntityType() ) {
			return null;
		}

		EntityPersister inverseSidePersister = mainSidePersister.getElementPersister();

		// process collection-typed properties of inverse side and try to find association back to main side
		for ( Type type : inverseSidePersister.getPropertyTypes() ) {
			if ( type.isCollectionType() ) {
				OgmCollectionPersister inverseCollectionPersister = getPersister( mainSidePersister.getFactory(), (CollectionType) type );
				if ( isCollectionMatching( mainSidePersister, inverseCollectionPersister ) ) {
					return inverseCollectionPersister;
				}
			}
		}

		return null;
	}

	/**
	 * Returns the role on the main side of the given association. If the collection is on the main side itself (or if
	 * it is an uni-directional association), its own unqualified role will be returned. If the collection is on the
	 * inverse side of a one-to-many or many-to-many association, the corresponding property on the main side will be
	 * determined and its name returned.
	 *
	 * @param collectionPersister the collection of interest
	 * @return the name of the property on the main side of the given association
	 */
	public static String getMainSidePropertyName(OgmCollectionPersister collectionPersister) {
		if ( !collectionPersister.isInverse() ) {
			return collectionPersister.getUnqualifiedRole();
		}

		Loadable mainSidePersister = (Loadable) collectionPersister.getElementPersister();

		for ( int i = 0; i < mainSidePersister.getPropertyNames().length; i++ ) {
			String candidateProperty = mainSidePersister.getPropertyNames()[i];
			Type type = mainSidePersister.getPropertyType( candidateProperty );

			// candidate is to-one association
			if ( type.isEntityType() ) {
				if ( Arrays.equals( collectionPersister.getKeyColumnNames(), mainSidePersister.getPropertyColumnNames( i ) ) ) {
					return candidateProperty;
				}
			}
			// candidate is to-many association
			else if ( type.isCollectionType() ) {
				OgmCollectionPersister mainSideCollectionPersister = getPersister( collectionPersister.getFactory(), (CollectionType) type );
				if ( isCollectionMatching( mainSideCollectionPersister, collectionPersister ) ) {
					return candidateProperty;
				}
			}
		}

		throw new HibernateException( "Couldn't determine main side role for collection " + collectionPersister.getRole() );
	}

	/**
	 * Checks whether table name and key column names of the given joinable and inverse collection persister match.
	 */
	private static boolean isCollectionMatching(Joinable mainSideJoinable, OgmCollectionPersister inverseSidePersister) {
		boolean isSameTable = mainSideJoinable.getTableName().equals( inverseSidePersister.getTableName() );

		if ( !isSameTable ) {
			return false;
		}

		return Arrays.equals( mainSideJoinable.getKeyColumnNames(), inverseSidePersister.getElementColumnNames() );
	}

	private static OgmCollectionPersister getPersister(SessionFactoryImplementor factory, CollectionType type) {
		return (OgmCollectionPersister) factory.getCollectionPersister( type.getRole() );
	}
}
