/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.ogm.id.impl;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;

import java.io.Serializable;
import java.util.Properties;

/**
 * <p>An identity-based identifier generator.
 * The underlying implementation uses {@link OgmTableGenerator}
 * to generate and use the identifier value itself rather
 * than using a generated value provided by the grid/NoSql
 * engine. Because unlike SQL engines, the grid/NoSql engines
 * do not support any such identifier generation. Hence this
 * identifier is not a post-commit value either.</p>
 *
 * Configuration parameters:
 * <table>
 * <tr>
 * <td><b>NAME</b></td>
 * <td><b>DEFAULT</b></td>
 * <td><b>DESCRIPTION</b></td>
 * </tr>
 * <tr>
 * <td>{@link #TABLE_NAME}</td>
 * <td>The name of the table to use to store/retrieve values</td>
 * </tr>
 * <tr>
 * <td>{@link #IDENTITY_NAME}</td>
 * <td>The name of the column to use to store/retrieve identity values</td>
 * </tr>
 * <tr>
 * <td>{@link OgmTableGenerator#VALUE_COLUMN_PARAM}</td>
 * <td>{@link OgmTableGenerator#DEF_VALUE_COLUMN}</td>
 * <td>The name of column which holds the sequence value for the given segment</td>
 * </tr>
 * <tr>
 * <td>{@link OgmTableGenerator#SEGMENT_COLUMN_PARAM}</td>
 * <td>{@link OgmTableGenerator#DEF_SEGMENT_COLUMN}</td>
 * <td>The name of the column which holds the segment key</td>
 * </tr>
 * <tr>
 * <td>{@link OgmTableGenerator#SEGMENT_VALUE_PARAM}</td>
 * <td>{@link OgmTableGenerator#DEF_SEGMENT_VALUE}</td>
 * <td>The value indicating which segment is used by this generator; refers to values in the {@link OgmTableGenerator#SEGMENT_COLUMN_PARAM} column</td>
 * </tr>
 * <tr>
 * <td>{@link OgmTableGenerator#SEGMENT_LENGTH_PARAM}</td>
 * <td>{@link OgmTableGenerator#DEF_SEGMENT_LENGTH}</td>
 * <td>The data length of the {@link OgmTableGenerator#SEGMENT_COLUMN_PARAM} column; used for schema creation</td>
 * </tr>
 * <tr>
 * <td>{@link OgmTableGenerator#INITIAL_PARAM}</td>
 * <td>{@link OgmTableGenerator#DEFAULT_INITIAL_VALUE}</td>
 * <td>The initial value to be stored for the given segment</td>
 * </tr>
 * <tr>
 * <td>{@link OgmTableGenerator#INCREMENT_PARAM}</td>
 * <td>{@link OgmTableGenerator#DEFAULT_INCREMENT_SIZE}</td>
 * <td>The increment size for the underlying segment; see the discussion on {@link org.hibernate.id.enhanced.Optimizer} for more details.</td>
 * </tr>
 * <tr>
 * <td>{@link OgmTableGenerator#OPT_PARAM}</td>
 * <td><i>depends on defined increment size</i></td>
 * <td>Allows explicit definition of which optimization strategy to use</td>
 * </tr>
 * </table>
 *
 * @see OgmTableGenerator
 *
 * @author Nabeel Ali Memon
 */
public class OgmIdentityGenerator implements PersistentIdentifierGenerator, Configurable {
    private static final String IDENTITY_NAME = "identity_tables";
    private static final String TABLE_NAME = "target_table";
    private final OgmTableGenerator tableGenerator;

    public OgmIdentityGenerator() {
        tableGenerator = new OgmTableGenerator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
        tableGenerator.setIdentifierType( type );
        tableGenerator.setTableName( tableGenerator.determineGeneratorTableName( params, dialect ) );
        tableGenerator.setSegmentColumnName( tableGenerator.determineSegmentColumnName( params, dialect ) );
        tableGenerator.setValueColumnName( tableGenerator.determineValueColumnName( params, dialect ) );
        tableGenerator.setInitialValue( tableGenerator.determineInitialValue( params ) );
        tableGenerator.setSegmentValueLength( tableGenerator.determineSegmentColumnSize( params ) );
        tableGenerator.setSegmentValue( determineIdentityColumnValue( params ) );
        tableGenerator.setIncrementSize( tableGenerator.determineIncrementSize( params ) );
        tableGenerator.setOptimizer( tableGenerator.determinePoolOptimization( params, tableGenerator.getIdentifierType() ) );
    }

    String determineIdentityColumnValue(Properties params) {
        return PropertiesHelper.getString( IDENTITY_NAME,
                                           params,
                                           params.getProperty( TABLE_NAME ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
        return new String[] {};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
        return new String[] {};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object generatorKey() {
        return tableGenerator.generatorKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        return tableGenerator.generate( session, object );
    }
}
