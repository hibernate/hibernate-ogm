/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.converter;

import java.util.UUID;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@Entity
public class Printer {

	@Id
	@GeneratedValue
	public UUID id;

	@Convert(converter = StringToUpperCaseConverter.class)
	public String name;

	@Convert(converter = MyStringToUpperCaseStringConverter.class)
	public MyString brand;

	@Convert(converter = StringToByteArrayConverter.class)
	public String description;

//	@Convert(converter = JpaConvertCustomTypeTest.URLToURLConverter.class)
//	public URL url;

}
