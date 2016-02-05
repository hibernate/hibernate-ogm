package org.hibernate.ogm.datastore.ignite;

import org.apache.ignite.binary.BinaryObjectException;
import org.jetbrains.annotations.Nullable;

public interface PortableObjectDelegate
{
    /**
     * Gets portable object type ID.
     *
     * @return Type ID.
     */
    public int typeId();
    /**
     * Gets field value.
     *
     * @param fieldName Field name.
     * @return Field value.
     * @throws PortableException In case of any other error.
     */
    @Nullable public <F> F field(String fieldName) throws BinaryObjectException;
    /**
     * Checks whether field is set.
     *
     * @param fieldName Field name.
     * @return {@code true} if field is set.
     */
    public boolean hasField(String fieldName);
    /**
     * Gets fully deserialized instance of portable object.
     *
     * @return Fully deserialized instance of portable object.
     * @throws PortableInvalidClassException If class doesn't exist.
     * @throws PortableException In case of any other error.
     */
    @Nullable public <T> T deserialize() throws BinaryObjectException;
    /**
     * Copies this portable object.
     *
     * @return Copy of this portable object.
     */
    public PortableObjectDelegate clone() throws CloneNotSupportedException;
//    /**
//     * Gets meta data for this portable object.
//     *
//     * @return Meta data.
//     * @throws PortableException In case of error.
//     */
//    @Nullable public PortableMetadata metaData() throws PortableException;
    
    public Object getInternalInstance();
}
