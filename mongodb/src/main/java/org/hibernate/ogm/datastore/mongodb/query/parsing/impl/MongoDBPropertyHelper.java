/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.PropertyHelper;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.query.parsing.impl.ParserPropertyHelper;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Property helper dealing with MongoDB.
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 */
public class MongoDBPropertyHelper extends ParserPropertyHelper implements PropertyHelper {

	private final SessionFactoryImplementor sessionFactory;

	public MongoDBPropertyHelper(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames) {
		super( sessionFactory, entityNames );
		this.sessionFactory = sessionFactory;
	}

	public String getColumnName(Class<?> entityType, List<String> propertyName) {
		return getColumnName( (OgmEntityPersister) getSessionFactory().getMetamodel().entityPersister( entityType ), propertyName );
	}

	public String getColumnName(String entityType, List<String> propertyPath) {
		return getColumnName( getPersister( entityType ), propertyPath );
	}

	@Override
	protected Type getPropertyType(String entityType, List<String> propertyPath) {
		Type propertyType = super.getPropertyType( entityType, propertyPath );
		if ( isElementCollection( propertyType ) ) {
			// For collection of elements we return the type of the collection
			return ( (CollectionType) propertyType ).getElementType( sessionFactory );
		}
		return propertyType;
	}

	@Override
	public Object convertToBackendType(String entityType, List<String> propertyPath, Object value) {
		Type propertyType = getPropertyType( entityType, propertyPath );
		if ( isElementCollection( propertyType ) ) {
			// For collection of elements we return the type of the collection
			propertyType = ( (CollectionType) propertyType ).getElementType( sessionFactory );
		}
		GridType ogmType = sessionFactory.getServiceRegistry().getService( TypeTranslator.class ).getType( propertyType );
		return ogmType.convertToBackendType( value, sessionFactory );
	}

	public String getColumnName(OgmEntityPersister persister, List<String> propertyPath) {
		String propertyName = StringHelper.join( propertyPath, "." );
		String identifierPropertyName = persister.getIdentifierPropertyName();
		if ( propertyName.equals( identifierPropertyName ) ) {
			return MongoDBDialect.ID_FIELDNAME;
		}
		String column = getColumn( persister, propertyPath );
		if ( propertyPath.size() > 1 && propertyPath.get( 0 ).equals( identifierPropertyName ) ) {
			column = MongoDBDialect.ID_FIELDNAME + "." + column.substring( propertyPath.get( 0 ).length() + 1 );
		}
		return column;
	}
}
