/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.PropertyHelper;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.query.parsing.impl.ParserPropertyHelper;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.type.Type;

/**
 * Property helper dealing with Neo4j.
 *
 * @author Davide D'Alto
 */
public class Neo4jPropertyHelper extends ParserPropertyHelper implements PropertyHelper {

	public Neo4jPropertyHelper(SessionFactoryImplementor sessionFactory, EntityNamesResolver entityNames) {
		super( sessionFactory, entityNames );
	}

	@Override
	public Object convertToBackendType(String entityType, List<String> propertyPath, Object value) {
		if ( value instanceof Neo4jQueryParameter ) {
			return value;
		}
		else {
			return super.convertToBackendType( entityType, propertyPath, value );
		}
	}

	public Object convertToLiteral(String entityType, List<String> propertyPath, Object value) {
		Type propertyType = getPropertyType( entityType, propertyPath );
		Object gridValue = convertToGridType( value, propertyType );
		return gridValue;
	}

	private Object convertToGridType(Object value, Type propertyType) {
		if ( value instanceof Neo4jQueryParameter ) {
			return value;
		}
		else {
			Tuple dummy = new Tuple();
			GridType gridType = typeTranslator().getType( propertyType );
			gridType.nullSafeSet( dummy, value, new String[] { "key" }, null );
			return dummy.get( "key" );
		}
	}

	private TypeTranslator typeTranslator() {
		return getSessionFactory().getServiceRegistry().getService( TypeTranslator.class );
	}

	public String getColumnName(Class<?> entityType, List<String> propertyName) {
		return getColumnName( (OgmEntityPersister) getSessionFactory().getEntityPersister( entityType.getName() ), propertyName );
	}

	public String getColumnName(String entityType, List<String> propertyPath) {
		return getColumnName( getPersister( entityType ), propertyPath );
	}

	public String getColumnName(OgmEntityPersister persister, List<String> propertyName) {
		return getColumn( persister, propertyName);
	}

	public String getEmbeddeColumnName(String entityType, List<String> propertyPath) {
		String columnName = getColumnName( entityType, propertyPath );
		columnName = columnName.substring( columnName.lastIndexOf( '.' ) + 1, columnName.length() );
		return columnName;
	}
}
