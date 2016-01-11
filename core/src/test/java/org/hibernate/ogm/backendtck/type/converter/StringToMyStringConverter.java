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
public class StringToMyStringConverter implements AttributeConverter<String, MyString> {

	@Override
	public MyString convertToDatabaseColumn(String attribute) {
		return attribute != null ? new MyString( attribute.toUpperCase() ) : null;
	}

	@Override
	public String convertToEntityAttribute(MyString dbData) {
		return dbData != null ? dbData.toString().toLowerCase() : null;
	}
}
