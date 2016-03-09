/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite;

import org.apache.ignite.binary.BinaryObjectException;
import org.jetbrains.annotations.Nullable;

public interface PortableBuilderDelegate {
    /**
     * Returns value assigned to the specified field.
     * If the value is a portable object instance of {@code GridPortableBuilder} will be returned,
     * which can be modified.
     * <p>
     * Collections and maps returned from this method are modifiable.
     *
     * @param name Field name.
     * @return Filed value.
     */
	<T> T getField(String name);

    /**
     * Sets field value.
     *
     * @param name Field name.
     * @param val Field value (cannot be {@code null}).
     * @see PortableObject#metaData()
     */
	PortableBuilderDelegate setField(String name, Object val);

    /**
     * Sets field value with value type specification.
     * <p>
     * Field type is needed for proper metadata update.
     *
     * @param name Field name.
     * @param val Field value.
     * @param type Field type.
     * @see PortableObject#metaData()
     */
	<T> PortableBuilderDelegate setField(String name, @Nullable T val, Class<? super T> type);

    /**
     * Sets field value.
     * <p>
     * This method should be used if field is portable object.
     *
     * @param name Field name.
     * @param builder Builder for object field.
     */
	PortableBuilderDelegate setField(String name, @Nullable PortableBuilderDelegate builder);

    /**
     * Removes field from this builder.
     *
     * @param fieldName Field name.
     * @return {@code this} instance for chaining.
     */
	PortableBuilderDelegate removeField(String fieldName);

    /**
     * Sets hash code for resulting portable object returned by {@link #build()} method.
     * <p>
     * If not set {@code 0} is used.
     *
     * @param hashCode Hash code.
     * @return {@code this} instance for chaining.
     */
	PortableBuilderDelegate hashCode(int hashCode);

    /**
     * Builds portable object.
     *
     * @return Portable object.
     * @throws PortableException In case of error.
     */
	PortableObjectDelegate build() throws BinaryObjectException;
}
