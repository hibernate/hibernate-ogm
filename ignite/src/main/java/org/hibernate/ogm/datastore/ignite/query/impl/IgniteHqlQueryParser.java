package org.hibernate.ogm.datastore.ignite.query.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlParser;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl.JavaConstantConverter;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.hql.internal.ast.tree.FromReferenceNode;
import org.hibernate.hql.internal.ast.tree.QueryNode;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.hql.internal.ast.util.NodeTraverser;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.type.Type;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

/**
 * Parser for Ignite Hql queries
 * 
 * @author Dmitriy Kozlov
 *
 */
public class IgniteHqlQueryParser {

	private static final Log log = LoggerFactory.getLogger();
	
	private final SessionFactoryImplementor factory;
	private final HqlSqlWalker walker;
	private final SelectClause selectClause;
	private final String query;
	private final Set<String> querySpaces;
	private final List<String> columnNames;
	
	public IgniteHqlQueryParser(String query, SessionFactoryImplementor factory) {
		this.query = query;
		this.factory = factory;
		this.walker = parseHqlSqlWalker();
		this.selectClause = walker.getSelectClause();
		Set<String> querySpaces = new HashSet<String>();
		for (Serializable querySpace : walker.getQuerySpaces()) {
			querySpaces.add((String)querySpace);
		}
		this.querySpaces = Collections.unmodifiableSet(querySpaces);
		List<String> columns = new ArrayList<String>();
		if (selectClause.getColumnNames() != null && selectClause.getColumnNames().length > 0) {
			for (int i = 0; i < selectClause.getColumnNames().length; i++) {
				columns.addAll(Arrays.asList(selectClause.getColumnNames()[i]));
			}
		}
		this.columnNames = columns;
	}
	
	private HqlSqlWalker parseHqlSqlWalker() {
		// took this code from QueryTranslatorImpl
		try {
			// PHASE 1 : Parse the HQL into an AST.
			final HqlParser parser = parse(query, factory);
			// PHASE 2 : Analyze the HQL AST, and produce an SQL AST.
			final QueryTranslatorImpl translator = new QueryTranslatorImpl(query, query, Collections.EMPTY_MAP, factory);
			return analyze(translator, parser, factory);
		}
		catch ( QueryException qe ) {
			if ( qe.getQueryString() == null ) {
				throw qe.wrapWithQueryString( query );
			}
			else {
				throw qe;
			}
		}
		catch ( ANTLRException e ) {
			// we do not actually propagate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			log.error( "Converted antlr.ANTLRException", e );
			throw new QueryException( e.getMessage(), query );
		}
	}
	
	public IgniteQueryDescriptor buildQueryDescriptor() {
		
		// took this code from QueryTranslatorImpl
		try {
			// PHASE 3 : Generate the SQL.
			final SqlGenerator gen = new SqlGenerator(factory);
			gen.statement(walker.getAST());
			String sql = gen.getSQL();
//			final SqlGenerator genFrom = new SqlGenerator(factory);
//			genFrom.from(((QueryNode)walker.getAST()).getFromClause());
//			AST whereClause = ((QueryNode)walker.getAST()).getWhereClause();
//			if (whereClause.getNumberOfChildren() > 0)
//				genFrom.whereClause(whereClause);
//			String fromSql = genFrom.getSQL();
//			final SqlGenerator orderGen = new SqlGenerator(factory); 
			
			String resultSql = sql;
			
			if (!selectClause.isScalarSelect()) {
				// Create query with fields _KEY and _VAL
//				String alias = ((QueryNode)walker.getAST()).getFromClause().getFromElement().getTableAlias();
				FromReferenceNode dotNode = (FromReferenceNode)((QueryNode)walker.getAST()).getSelectClause().getFirstChild();
				String alias = dotNode.getFromElement().getTableAlias(); 
				StringBuffer buf = new StringBuffer("select ");
				buf.append(alias).append("._KEY").append(" , ");
				buf.append(alias).append("._VAL ");
				String fromSql = sql.substring(sql.indexOf(" from ") + 1);
				buf.append(fromSql);
				resultSql = buf.toString();
			}
			else {
				for (Type type : selectClause.getQueryReturnTypes()){
					if (type.isEntityType() || type.isAssociationType() || type.isCollectionType() || type.isComponentType())
						throw new IgniteHibernateException("Query with entity in projections are not supported");
				}
			}
			
			return new IgniteQueryDescriptor(query, resultSql, selectClause, querySpaces);
		}
		catch ( RecognitionException e ) {
			// we do not actually propagate ANTLRExceptions as a cause, so
			// log it here for diagnostic purposes
			log.error( "Converted antlr.RecognitionException", e );
			throw QuerySyntaxException.convert( e, query );
		}
	}
	
	private HqlParser parse(String hql, SessionFactoryImplementor sessionFactory) throws TokenStreamException, RecognitionException {
		// Parse the query string into an HQL AST.
		final HqlParser parser = HqlParser.getInstance( hql );
		parser.setFilter(true);

		parser.statement();

		final AST hqlAst = parser.getAST();

		final NodeTraverser walker = new NodeTraverser( new JavaConstantConverter( sessionFactory ) );
		walker.traverseDepthFirst( hqlAst );

		ASTPrinter HQL_TOKEN_PRINTER = new ASTPrinter( HqlTokenTypes.class );
		log.debug(HQL_TOKEN_PRINTER.showAsString(hqlAst, "--- HQL AST ---"));

		parser.getParseErrorHandler().throwQueryException();
		return parser;
	}
	
	private HqlSqlWalker analyze(QueryTranslatorImpl translator, HqlParser parser, SessionFactoryImplementor sessionFactory) throws QueryException, RecognitionException {
		final HqlSqlWalker w = new HqlSqlWalker(translator, sessionFactory, parser, sessionFactory.getSessionFactoryOptions().getQuerySubstitutions(), null );
		final AST hqlAst = parser.getAST();

		// Transform the tree.
		w.statement( hqlAst );
		
		ASTPrinter SQL_TOKEN_PRINTER = new ASTPrinter( SqlTokenTypes.class );
		log.debug( SQL_TOKEN_PRINTER.showAsString( w.getAST(), "--- SQL AST ---" ) );
		

		w.getParseErrorHandler().throwQueryException();

		return w;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}
	
}
