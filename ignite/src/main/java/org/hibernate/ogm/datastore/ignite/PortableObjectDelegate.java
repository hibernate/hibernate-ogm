/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite;

import org.apache.ignite.binary.BinaryObjectException;
import org.jetbrains.annotations.Nullable;

public interface PortableObjectDelegate {
    /**
     * Gets portable object type ID.
     *
     * @return Type ID.
     */
	int typeId();
    /**
     * Gets field value.
     *
     * @param fieldName Field name.
     * @return Field value.
     * @throws PortableException In case of any other error.
     */
	@Nullable <F> F field(String fieldName) throws BinaryObjectException;
    /**
     * Checks whether field is set.
     *
     * @param fieldName Field name.
     * @return {@code true} if field is set.
     */
	boolean hasField(String fieldName);
    /**
     * Gets fully deserialized instance of portable object.
     *
     * @return Fully deserialized instance of portable object.
     * @throws PortableInvalidClassException If class doesn't exist.
     * @throws PortableException In case of any other error.
     */
	@Nullable <T> T deserialize() throws BinaryObjectException;
    /**
     * Copies this portable object.
     *
     * @return Copy of this portable object.
     */
	PortableObjectDelegate clone() throws CloneNotSupportedException ;

//    /**
//     * Gets meta data for this portable object.
//     *
//     * @return Meta data.
//     * @throws PortableException In case of error.
//     */
//    @Nullable public PortableMetadata metaData() throws PortableException;

	Object getInternalInstance();
}
