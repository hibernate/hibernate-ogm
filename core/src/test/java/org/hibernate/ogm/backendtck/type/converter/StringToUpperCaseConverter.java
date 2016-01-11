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
public class StringToUpperCaseConverter implements AttributeConverter<String, String> {

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return attribute != null ? attribute.toUpperCase() : null;
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return dbData != null ? dbData.toLowerCase() : null;
	}
}
