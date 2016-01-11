/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.converter;

import javax.persistence.AttributeConverter;

/**
 * @author Gunnar Morling
 */
public class MyStringToUpperCaseStringConverter implements AttributeConverter<MyString, String> {

	@Override
	public String convertToDatabaseColumn(MyString attribute) {
		return attribute != null ? attribute.toString().toUpperCase() : null;
	}

	@Override
	public MyString convertToEntityAttribute(String dbData) {
		return dbData != null ? new MyString( dbData.toLowerCase() ) : null;
	}
}
