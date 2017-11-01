/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure.impl;

import java.lang.invoke.MethodHandles;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.procedure.ParameterBind;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.type.Type;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NoSQLProcedureParameterRegistration<T> implements ParameterRegistration<T> {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final NoSQLProcedureCallImpl procedureCall;

	private final Integer position;
	private final String name;

	private final ParameterMode mode;
	private final Class<T> type;

	private ParameterBindImpl bind;
	private boolean passNulls;

	private int startIndex;
	private Type hibernateType;

	public NoSQLProcedureParameterRegistration(NoSQLProcedureCallImpl procedureCall,Integer position,
			String name, ParameterMode mode, Class<T> type) {
		this.procedureCall = procedureCall;
		this.position = position;
		this.name = name;
		this.mode = mode;
		this.type = type;
		this.hibernateType = procedureCall.getSession().getFactory().getTypeResolver().heuristicType( type.getName() );
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ParameterMode getMode() {
		return mode;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public ParameterBindImpl getBind() {
		return bind;
	}

	public void setBind(ParameterBindImpl bind) {
		this.bind = bind;
	}

	public boolean isPassNulls() {
		return passNulls;
	}

	public void setPassNulls(boolean passNulls) {
		this.passNulls = passNulls;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public Type getHibernateType() {
		return hibernateType;
	}

	@Override
	public void setHibernateType(Type hibernateType) {
		this.hibernateType = hibernateType;
	}

	@Override
	public void enablePassingNulls(boolean enabled) {
		this.passNulls = enabled;
	}

	@Override
	public void bindValue(T value) {
		validateBindability();
		this.bind = new ParameterBindImpl<T>( value );
	}

	@Override
	public void bindValue(T value, TemporalType explicitTemporalType) {

	}
	private void validateBindability() {
		if ( ! canBind() ) {
			throw new ParameterMisuseException( "Cannot bind value to non-input parameter : " + this );
		}
	}

	private boolean canBind() {
		return mode == ParameterMode.IN || mode == ParameterMode.INOUT;
	}

	public class ParameterBindImpl<T> implements ParameterBind<T> {
		private final T value;
		private final TemporalType explicitTemporalType;

		ParameterBindImpl(T value) {
			this( value, null );
		}

		ParameterBindImpl(T value, TemporalType explicitTemporalType) {
			this.value = value;
			this.explicitTemporalType = explicitTemporalType;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public TemporalType getExplicitTemporalType() {
			return explicitTemporalType;
		}
	}

}
