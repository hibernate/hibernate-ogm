/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.converter;

import java.nio.charset.StandardCharsets;

import javax.persistence.AttributeConverter;

/**
 * @author Gunnar Morling
 */
public class StringToByteArrayConverter implements AttributeConverter<String, byte[]> {

	@Override
	public byte[] convertToDatabaseColumn(String attribute) {
		return attribute != null ? attribute.getBytes( StandardCharsets.UTF_8 ) : null;
	}

	@Override
	public String convertToEntityAttribute(byte[] dbData) {
		return dbData != null ? new String( dbData, StandardCharsets.UTF_8 ) : null;
	}
}
