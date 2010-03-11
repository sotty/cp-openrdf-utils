/*
 * Copyright (c) 2009-2010 Clark & Parsia, LLC. <http://www.clarkparsia.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.clarkparsia.openrdf.query.builder.impl;

import org.openrdf.model.Value;
import org.openrdf.query.algebra.BinaryTupleOperator;
import org.openrdf.query.algebra.Extension;
import org.openrdf.query.algebra.ExtensionElem;
import org.openrdf.query.algebra.Join;
import org.openrdf.query.algebra.LeftJoin;
import org.openrdf.query.algebra.MultiProjection;
import org.openrdf.query.algebra.Projection;
import org.openrdf.query.algebra.ProjectionElem;
import org.openrdf.query.algebra.ProjectionElemList;
import org.openrdf.query.algebra.Slice;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UnaryTupleOperator;

import org.openrdf.query.algebra.ValueConstant;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.algebra.helpers.StatementPatternCollector;
import org.openrdf.query.algebra.helpers.VarNameCollector;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.clarkparsia.openrdf.query.builder.QueryBuilder;
import com.clarkparsia.openrdf.query.builder.Group;
import com.clarkparsia.openrdf.query.builder.GroupBuilder;

/**
 * <p>Base implementation of a QueryBuilder.</p>
 *
 * @author Michael Grove
 * @since 0.2
 * @version 0.2.1
 */
public class AbstractQueryBuilder<T extends ParsedQuery> implements QueryBuilder<T> {

	private List<String> mProjectionVars = new ArrayList<String>();

	// this is a bit of a hack making this protected so the construct query impl can access it.
	// would be better to encapsulate building the projection element up so the subclasses just handle it.
	protected List<StatementPattern> mProjectionPatterns = new ArrayList<StatementPattern>();

	private List<Group> mQueryAtoms = new ArrayList<Group>();

	private int mLimit = -1;
	private int mOffset = -1;

    private T mQuery;

    AbstractQueryBuilder(T theQuery) {
        mQuery = theQuery;
    }

	/**
	 * @inheritDoc
	 */
	public void reset() {
		mLimit = mOffset = -1;
		mProjectionVars.clear();
		mQueryAtoms.clear();
		mProjectionPatterns.clear();
	}

	/**
	 * @inheritDoc
	 */
	public T query() {
		UnaryTupleOperator aRoot = null;
		UnaryTupleOperator aCurr = null;

		if (mLimit != -1 || mOffset != -1) {
			Slice aSlice = new Slice();
			if (mLimit != -1) {
				aSlice.setLimit(mLimit);
			}
			if (mOffset != -1) {
				aSlice.setOffset(mOffset);
			}

			aRoot = aCurr = aSlice;
		}

		TupleExpr aJoin = join();

        if (mQuery instanceof ParsedTupleQuery && mProjectionVars.isEmpty()) {
            VarNameCollector aCollector = new VarNameCollector();

            aJoin.visit(aCollector);

            mProjectionVars.addAll(aCollector.getVarNames());
        }
        else if (mQuery instanceof ParsedGraphQuery && mProjectionPatterns.isEmpty()) {
            StatementPatternCollector aCollector = new StatementPatternCollector();

            aJoin.visit(aCollector);

            mProjectionPatterns.addAll(aCollector.getStatementPatterns());
        }

		UnaryTupleOperator aProjection = projection();

		if (aRoot == null) {
			aRoot = aCurr = aProjection;
		}
		else {
			aCurr.setArg(aProjection);
		}

		if (aProjection.getArg() == null) {
			aCurr = aProjection;
		}
		else {
			// I think this is always a safe cast
			aCurr = (UnaryTupleOperator) aProjection.getArg();
		}

		aCurr.setArg(aJoin);

		mQuery.setTupleExpr(aRoot);

        return mQuery;
	}

	private TupleExpr join() {
		if (mQueryAtoms.isEmpty()) {
			throw new RuntimeException("Can't have an empty or missing join.");
		}
		else if (mQueryAtoms.size() == 1) {
			return mQueryAtoms.get(0).expr();
		}
		else {
			return groupAsJoin(mQueryAtoms);
		}
	}

	private UnaryTupleOperator projection() {
		if (!mProjectionPatterns.isEmpty()) {
			return multiProjection();
		}
		else {
			Extension aExt = null;

			ProjectionElemList aList = new ProjectionElemList();

			for (String aVar : mProjectionVars) {
				aList.addElement(new ProjectionElem(aVar));
			}

			Projection aProjection = new Projection();
			aProjection.setProjectionElemList(aList);

			if (aExt != null) {
				aProjection.setArg(aExt);
			}

			return aProjection;
		}
	}

	private UnaryTupleOperator multiProjection() {
		MultiProjection aProjection = new MultiProjection();

        Extension aExt = null;

        for (StatementPattern aPattern : mProjectionPatterns) {
            ProjectionElemList aList = new ProjectionElemList();

            aList.addElement(new ProjectionElem(aPattern.getSubjectVar().getName(), "subject"));
            aList.addElement(new ProjectionElem(aPattern.getPredicateVar().getName(), "predicate"));
            aList.addElement(new ProjectionElem(aPattern.getObjectVar().getName(), "object"));

            if (aPattern.getSubjectVar().hasValue()) {
                if (aExt == null) {
                    aExt = new Extension();
                }

                aExt.addElements(new ExtensionElem(new ValueConstant(aPattern.getSubjectVar().getValue()),
                                                   aPattern.getSubjectVar().getName()));
            }

            if (aPattern.getPredicateVar().hasValue()) {
                if (aExt == null) {
                    aExt = new Extension();
                }

                aExt.addElements(new ExtensionElem(new ValueConstant(aPattern.getPredicateVar().getValue()),
                                                   aPattern.getPredicateVar().getName()));
            }

            if (aPattern.getObjectVar().hasValue()) {
                if (aExt == null) {
                    aExt = new Extension();
                }

                aExt.addElements(new ExtensionElem(new ValueConstant(aPattern.getObjectVar().getValue()),
                                                   aPattern.getObjectVar().getName()));
            }
            
            aProjection.addProjection(aList);
        }

        if (aExt != null) {
            aProjection.setArg(aExt);
        }

        return aProjection;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addProjectionVar(String... theNames) {
		mProjectionVars.addAll(Arrays.asList(theNames));
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public GroupBuilder<T> group() {
		return new GroupBuilder<T>(this, false, null);
	}

	/**
	 * @inheritDoc
	 */
	public GroupBuilder<T> optional() {
		return new GroupBuilder<T>(this, true, null);
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> limit(int theLimit) {
		mLimit = theLimit;
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> offset(int theOffset) {
		mOffset = theOffset;
		return this;
	}

	/**
	 * @inheritDoc
	 */
	public QueryBuilder<T> addGroup(Group theGroup) {
		mQueryAtoms.add(theGroup);
        return this;
	}


    private TupleExpr groupAsJoin(List<Group> theList) {
		BinaryTupleOperator aJoin = new Join();

		for (Group aGroup : theList) {
			TupleExpr aExpr = aGroup.expr();

			if (aGroup.isOptional()) {
				LeftJoin lj = new LeftJoin();

				TupleExpr aLeft = joinOrExpr(aJoin);

				if (aLeft != null) {
					lj.setLeftArg(aLeft);
					lj.setRightArg(aExpr);

					aJoin = lj;

					continue;
				}
			}

			if (aJoin.getLeftArg() == null) {
				aJoin.setLeftArg(aExpr);
			}
			else if (aJoin.getRightArg() == null) {
				aJoin.setRightArg(aExpr);
			}
			else {
				Join aNewJoin = new Join();

				aNewJoin.setLeftArg(aJoin);
				aNewJoin.setRightArg(aExpr);

				aJoin = aNewJoin;
			}
		}

		return joinOrExpr(aJoin);
	}

	private TupleExpr joinOrExpr(BinaryTupleOperator theExpr) {
		if (theExpr.getLeftArg() != null && theExpr.getRightArg() == null) {
			return theExpr.getLeftArg();
		}
		else if (theExpr.getLeftArg() == null && theExpr.getRightArg() != null) {
			return theExpr.getRightArg();
		}
		else if (theExpr.getLeftArg() == null && theExpr.getRightArg() == null) {
			return null;
		}
		else {
			return theExpr;
		}
	}
}