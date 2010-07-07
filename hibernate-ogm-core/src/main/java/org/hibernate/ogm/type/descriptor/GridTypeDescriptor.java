package org.hibernate.ogm.type.descriptor;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for the <tt>grid</tt> side of a value mapping.
 *
 * @author Emmanuel Bernard
 */
public interface GridTypeDescriptor extends Serializable {
	public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor);
}
