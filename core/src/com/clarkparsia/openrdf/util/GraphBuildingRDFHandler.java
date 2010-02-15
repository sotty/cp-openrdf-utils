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

package com.clarkparsia.openrdf.util;

import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.model.Statement;
import com.clarkparsia.openrdf.ExtGraph;

/**
 * <p>Implementation of an RDFHandler which collects statements from the handler events and puts them into a Graph object.</p>
 * @author Michael Grove
 * @since 0.1
 */
public class GraphBuildingRDFHandler extends RDFHandlerBase {

	/**
	 * The graph to collect statements in
	 */
	private ExtGraph mGraph = new ExtGraph();

	/**
	 * @inheritDoc
	 */
	@Override
	public void handleStatement(final Statement theStatement) throws RDFHandlerException {
		mGraph.add(theStatement);
	}

	/**
	 * Return the graph built from events fired to this handler
	 * @return the graph
	 */
	public ExtGraph getGraph() {
		return mGraph;
	}

	/**
	 * Clear the underlying graph of all collected statements
	 */
	public void clear() {
		mGraph.clear();
	}
}