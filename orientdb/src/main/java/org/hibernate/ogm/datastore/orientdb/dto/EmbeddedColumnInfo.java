/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.dto;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The class is presentation of column of embedded entity.
 * <p>
 * Embedded column has name like 'class1.class2.field1'. The class separates names of class and name of property of leaf
 * class
 * </p>
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class EmbeddedColumnInfo {

	private final LinkedList<String> classNames;
	private final String propertyName;

	/**
	 * Contractor
	 *
	 * @param sourcePropertyName source field name (like 'class1.field2')
	 */
	public EmbeddedColumnInfo(String sourcePropertyName) {
		String[] parts = sourcePropertyName.split( "\\." );
		classNames = new LinkedList<>( Arrays.asList( parts ) );
		propertyName = classNames.getLast();
		classNames.removeLast();
	}

	/**
	 * Get classes in source property name
	 *
	 * @return list of classes
	 */
	public List<String> getClassNames() {
		return classNames;
	}

	/**
	 * Get property name in leaf class
	 *
	 * @return property name
	 */
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public String toString() {
		return "EmbeddedColumnInfo{" + "classNames=" + classNames + ", propertyName=" + propertyName + "}'";
	}
}
